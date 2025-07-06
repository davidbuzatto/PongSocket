package pong.gem;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pong.componentes.Bola;
import pong.componentes.Jogador;
import pong.componentes.TipoJogador;

/**
 * Cliente reescrito para enviar e receber dados em threads separadas,
 * e implementar interpolação para o movimento suave de objetos controlados pelo servidor.
 *
 * @author Prof. Dr. David Buzatto (Adaptado por Gemini) - não funciona :D:D:D
 */
public class Cliente extends EngineFrame {

    private int id; // ID do cliente (para depuração, não usado na comunicação UDP direta)

    private int porta;
    private String host;
    private InetSocketAddress enderecoServidor;
    private DatagramChannel cliente;
    private ByteBuffer buffer;

    private volatile boolean executando;

    private Bola bola;
    private Jogador jogador; // O próprio jogador
    private TipoJogador tipo;
    private Jogador adversario; // O jogador adversário

    // Fila thread-safe para armazenar os estados de jogo recebidos do servidor.
    private ConcurrentLinkedQueue<GameState> filaEstadosJogo;

    // Executor para agendar tarefas de rede (envio e recebimento).
    private ScheduledExecutorService scheduler;

    // Taxa de envio de entrada do cliente para o servidor (em milissegundos).
    private static final int SEND_INPUT_RATE_MS = 20; // 50 vezes por segundo

    // Variáveis para interpolação da bola e do adversário
    private GameState estadoAnteriorBolaAdversario;
    private GameState estadoAtualBolaAdversario;
    private long timestampRecebimentoEstado; // Tempo em que o estadoAtualBolaAdversario foi recebido

    public Cliente(int porta, String host, int tamanhoBuffer, TipoJogador tipo) {
        super(800, 450, "Pong - Cliente " + tipo.name(), 60, true);
        this.id = tipo.valor; // Usando o valor do TipoJogador como ID simples
        this.porta = porta;
        this.host = host;
        this.tipo = tipo;
        this.buffer = ByteBuffer.allocate(tamanhoBuffer);
        this.filaEstadosJogo = new ConcurrentLinkedQueue<>();
        iniciarCliente();
    }

    @Override
    public void create() {
        // Inicializa a bola e o adversário com valores padrão.
        // Suas posições serão atualizadas pelos dados do servidor.
        bola = new Bola(
            new Vector2(getScreenWidth() / 2, getScreenHeight() / 2),
            0, // Raio inicial 0, será atualizado pelo servidor
            new Vector2(0, 0),
            WHITE
        );

        adversario = new Jogador(
            new Vector2(),
            new Vector2(),
            new Vector2(),
            WHITE,
            TipoJogador.SERVIDOR // Representa o adversário controlado pelo servidor
        );
    }

    /**
     * Cria o objeto Jogador para este cliente.
     */
    private void criarJogador() {
        int largJogador = 30;
        int altJogador = 150;

        if (tipo == TipoJogador.UM) {
            jogador = new Jogador(
                new Vector2(largJogador, getScreenHeight() / 2 - altJogador / 2),
                new Vector2(largJogador, altJogador),
                new Vector2(0, 200), // Velocidade de movimento do jogador
                WHITE,
                tipo
            );
        } else if (tipo == TipoJogador.DOIS) {
            jogador = new Jogador(
                new Vector2(getScreenWidth() - largJogador * 2, getScreenHeight() / 2 - altJogador / 2),
                new Vector2(largJogador, altJogador),
                new Vector2(0, 200),
                WHITE,
                tipo
            );
        }
    }

    @Override
    public void update(double delta) {
        
        criarJogador();
        
        // Atualiza a posição do próprio jogador com base na entrada local.
        // Isso é a "predição" do lado do cliente.
        jogador.atualizar(this, delta);

        // Processa os estados de jogo recebidos do servidor.
        processarEstadosJogoRecebidos();

        // Interpola a posição da bola e do adversário para um movimento suave.
        interpolarEstadoJogo(delta);
        
    }

    @Override
    public void draw() {
        clearBackground(BLACK);
        bola.desenhar(this);
        jogador.desenhar(this);
        adversario.desenhar(this);
    }

    /**
     * Inicia o cliente UDP e as threads de rede.
     */
    private void iniciarCliente() {
        if (!executando) {
            executando = true;
            try {
                enderecoServidor = new InetSocketAddress(InetAddress.getByName(host), porta);
                cliente = DatagramChannel.open();
                cliente.configureBlocking(false); // Canal não bloqueante

                // Inicializa o scheduler para gerenciar as threads de rede
                scheduler = Executors.newScheduledThreadPool(2); // Uma para enviar, outra para receber

                // Thread para enviar dados do jogador para o servidor
                scheduler.scheduleAtFixedRate(this::enviarDadosJogador, 0, SEND_INPUT_RATE_MS, TimeUnit.MILLISECONDS);

                // Thread para receber dados do servidor
                scheduler.scheduleAtFixedRate(this::receberDadosServidor, 0, 10, TimeUnit.MILLISECONDS);

                System.out.println("Cliente " + tipo.name() + " iniciado.");

            } catch (IOException exc) {
                System.err.println("Erro ao iniciar o cliente: " + exc.getMessage());
                exc.printStackTrace();
                executando = false;
            }
        }
    }

    /**
     * Thread de envio: envia a posição do jogador para o servidor.
     */
    private void enviarDadosJogador() {
        if (cliente == null || jogador == null) {
            return;
        }

        try {
            // Prepara o buffer com os dados do jogador
            // Formato: tipoJogador (int), x (double), y (double), largura (double), altura (double)
            ByteBuffer sendBuffer = ByteBuffer.allocate(4 * 5); // 5 doubles = 20 bytes

            sendBuffer.putInt(jogador.tipo.valor);
            sendBuffer.putDouble((double) jogador.pos.x);
            sendBuffer.putDouble((double) jogador.pos.y);
            sendBuffer.putDouble((double) jogador.dim.x);
            sendBuffer.putDouble((double) jogador.dim.y);

            sendBuffer.flip(); // Prepara o buffer para leitura

            cliente.send(sendBuffer, enderecoServidor);

        } catch (IOException exc) {
            System.err.println("Erro ao enviar dados (cliente): " + exc.getMessage());
        }
    }

    /**
     * Thread de recebimento: recebe dados do servidor e os enfileira.
     */
    private void receberDadosServidor() {
        try {
            buffer.clear();
            cliente.receive(buffer); // Não precisamos do remetente, pois só esperamos do servidor

            if (buffer.position() > 0) { // Se algo foi recebido
                buffer.flip();
                // Decodifica os dados binários do servidor
                // Formato: bola.x, bola.y, bola.raio, jogador1.x, jogador1.y, jogador1.larg, jogador1.alt,
                //          jogador2.x, jogador2.y, jogador2.larg, jogador2.alt (todos doubles)
                if (buffer.remaining() >= (4 * 11)) { // 11 doubles = 44 bytes
                    double bolaX = buffer.getFloat();
                    double bolaY = buffer.getFloat();
                    double bolaRaio = buffer.getFloat();

                    double jog1X = buffer.getFloat();
                    double jog1Y = buffer.getFloat();
                    double jog1Larg = buffer.getFloat();
                    double jog1Alt = buffer.getFloat();

                    double jog2X = buffer.getFloat();
                    double jog2Y = buffer.getFloat();
                    double jog2Larg = buffer.getFloat();
                    double jog2Alt = buffer.getFloat();

                    // Adiciona o estado à fila para ser processado na thread principal do jogo
                    filaEstadosJogo.offer(new GameState(
                        bolaX, bolaY, bolaRaio,
                        jog1X, jog1Y, jog1Larg, jog1Alt,
                        jog2X, jog2Y, jog2Larg, jog2Alt
                    ));
                }
            }
        } catch (IOException exc) {
            // Não imprima stack trace para cada erro de "nada para receber", pois é esperado.
            // Apenas para erros reais de I/O.
            // System.err.println("Erro ao receber dados (cliente): " + exc.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao processar dados recebidos (cliente): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processa os estados de jogo recebidos e atualiza os estados para interpolação.
     */
    private void processarEstadosJogoRecebidos() {
        GameState latestState = null;
        while (!filaEstadosJogo.isEmpty()) {
            latestState = filaEstadosJogo.poll();
        }

        if (latestState != null) {
            estadoAnteriorBolaAdversario = estadoAtualBolaAdversario;
            estadoAtualBolaAdversario = latestState;
            timestampRecebimentoEstado = System.currentTimeMillis();

            // Se não houver estado anterior, inicializa com o estado atual
            if (estadoAnteriorBolaAdversario == null) {
                estadoAnteriorBolaAdversario = estadoAtualBolaAdversario;
            }
        }
    }

    /**
     * Interpola o estado da bola e do adversário para suavizar o movimento.
     * Chamado no loop principal do jogo (update()).
     */
    private void interpolarEstadoJogo(double delta) {
        if (estadoAtualBolaAdversario == null) {
            return; // Nenhum estado recebido ainda
        }

        // Calcula o fator de interpolação (0.0 a 1.0)
        // Quanto mais próximo de 1.0, mais perto do estado atual estamos.
        long tempoDesdeRecebimento = System.currentTimeMillis() - timestampRecebimentoEstado;
        // UPDATE_RATE_MS é a taxa de envio do servidor.
        // Um pequeno buffer (ex: +10ms) pode ajudar a garantir que a interpolação cubra o próximo pacote.
        double t = Math.min(1.0f, (double) tempoDesdeRecebimento / (Servidor.UPDATE_RATE_MS + 10));

        // Interpola a bola
        if (estadoAnteriorBolaAdversario != null) {
            bola.pos.x = estadoAnteriorBolaAdversario.bolaX + (estadoAtualBolaAdversario.bolaX - estadoAnteriorBolaAdversario.bolaX) * t;
            bola.pos.y = estadoAnteriorBolaAdversario.bolaY + (estadoAtualBolaAdversario.bolaY - estadoAnteriorBolaAdversario.bolaY) * t;
            bola.raio = (int) estadoAtualBolaAdversario.bolaRaio; // Raio geralmente não interpola
        } else {
            // Se não há estado anterior, apenas define a posição atual
            bola.pos.x = estadoAtualBolaAdversario.bolaX;
            bola.pos.y = estadoAtualBolaAdversario.bolaY;
            bola.raio = (int) estadoAtualBolaAdversario.bolaRaio;
        }


        // Interpola o adversário
        if (tipo == TipoJogador.UM) { // Se este cliente é o Jogador 1, o adversário é o Jogador 2
            if (estadoAnteriorBolaAdversario != null) {
                adversario.pos.x = estadoAnteriorBolaAdversario.jog2X + (estadoAtualBolaAdversario.jog2X - estadoAnteriorBolaAdversario.jog2X) * t;
                adversario.pos.y = estadoAnteriorBolaAdversario.jog2Y + (estadoAtualBolaAdversario.jog2Y - estadoAnteriorBolaAdversario.jog2Y) * t;
                adversario.dim.x = estadoAtualBolaAdversario.jog2Larg;
                adversario.dim.y = estadoAtualBolaAdversario.jog2Alt;
            } else {
                adversario.pos.x = estadoAtualBolaAdversario.jog2X;
                adversario.pos.y = estadoAtualBolaAdversario.jog2Y;
                adversario.dim.x = estadoAtualBolaAdversario.jog2Larg;
                adversario.dim.y = estadoAtualBolaAdversario.jog2Alt;
            }
        } else { // Se este cliente é o Jogador 2, o adversário é o Jogador 1
            if (estadoAnteriorBolaAdversario != null) {
                adversario.pos.x = estadoAnteriorBolaAdversario.jog1X + (estadoAtualBolaAdversario.jog1X - estadoAnteriorBolaAdversario.jog1X) * t;
                adversario.pos.y = estadoAnteriorBolaAdversario.jog1Y + (estadoAtualBolaAdversario.jog1Y - estadoAnteriorBolaAdversario.jog1Y) * t;
                adversario.dim.x = estadoAtualBolaAdversario.jog1Larg;
                adversario.dim.y = estadoAtualBolaAdversario.jog1Alt;
            } else {
                adversario.pos.x = estadoAtualBolaAdversario.jog1X;
                adversario.pos.y = estadoAtualBolaAdversario.jog1Y;
                adversario.dim.x = estadoAtualBolaAdversario.jog1Larg;
                adversario.dim.y = estadoAtualBolaAdversario.jog1Alt;
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        executando = false;
        if (scheduler != null) {
            scheduler.shutdownNow(); // Encerra todas as threads agendadas
        }
        if (cliente != null) {
            try {
                cliente.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o cliente: " + e.getMessage());
            }
        }
    }

    /**
     * Classe interna para encapsular o estado completo do jogo recebido do servidor.
     */
    private static class GameState {
        
        double bolaX, bolaY, bolaRaio;
        double jog1X, jog1Y, jog1Larg, jog1Alt;
        double jog2X, jog2Y, jog2Larg, jog2Alt;

        public GameState(double bolaX, double bolaY, double bolaRaio,
                         double jog1X, double jog1Y, double jog1Larg, double jog1Alt,
                         double jog2X, double jog2Y, double jog2Larg, double jog2Alt) {
            this.bolaX = bolaX;
            this.bolaY = bolaY;
            this.bolaRaio = bolaRaio;
            this.jog1X = jog1X;
            this.jog1Y = jog1Y;
            this.jog1Larg = jog1Larg;
            this.jog1Alt = jog1Alt;
            this.jog2X = jog2X;
            this.jog2Y = jog2Y;
            this.jog2Larg = jog2Larg;
            this.jog2Alt = jog2Alt;
        }
    }
}
