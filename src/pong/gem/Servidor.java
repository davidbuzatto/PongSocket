package pong.gem;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pong.componentes.Bola;
import pong.componentes.Jogador;
import pong.componentes.TipoJogador;

/**
 * Servidor reescrito para lidar com a comunicação de rede em threads separadas,
 * atuar como fonte autoritária do estado do jogo e usar um formato de dados binário.
 *
 * @author Prof. Dr. David Buzatto (Adaptado por Gemini) - não funciona :D:D:D
 */
public class Servidor extends EngineFrame {

    private int porta;
    private DatagramChannel servidor;
    private ByteBuffer buffer;

    private volatile boolean executando;

    private Bola bola;
    private Jogador jogador1;
    private Jogador jogador2;

    // Mapa para armazenar os endereços dos clientes.
    // Usamos ConcurrentHashMap para segurança de thread.
    private Map<TipoJogador, SocketAddress> clientesConectados;

    // Fila thread-safe para armazenar as entradas dos clientes recebidas pela thread de rede.
    private ConcurrentLinkedQueue<ClientInput> filaEntradasClientes;

    // Executor para agendar tarefas de rede (recebimento e envio).
    private ScheduledExecutorService scheduler;

    // Taxa de atualização do servidor para os clientes (em milissegundos).
    public static final int UPDATE_RATE_MS = 30; // Aproximadamente 33 FPS de atualização de rede

    public Servidor(int porta, int tamanhoBuffer) {
        super(800, 450, "Pong - Servidor", 60, true);
        this.porta = porta;
        this.buffer = ByteBuffer.allocate(tamanhoBuffer);
        this.clientesConectados = new ConcurrentHashMap<>();
        this.filaEntradasClientes = new ConcurrentLinkedQueue<>();
        iniciarServidor();
    }

    @Override
    public void create() {
        // Inicializa a bola e os jogadores no servidor.
        // O servidor é a autoridade sobre o estado desses objetos.
        bola = new Bola(
            new Vector2(getScreenWidth() / 2, getScreenHeight() / 2),
            15,
            new Vector2(200, 200), // Velocidade inicial da bola
            WHITE
        );

        // Inicializa jogadores com posições e dimensões padrão.
        // As posições iniciais podem ser ajustadas para o jogo.
        int largJogador = 30;
        int altJogador = 150;

        jogador1 = new Jogador(
            new Vector2(largJogador, getScreenHeight() / 2 - altJogador / 2),
            new Vector2(largJogador, altJogador),
            new Vector2(0, 200),
            WHITE,
            TipoJogador.UM // Servidor gerencia o TipoJogador
        );

        jogador2 = new Jogador(
            new Vector2(getScreenWidth() - largJogador * 2, getScreenHeight() / 2 - altJogador / 2),
            new Vector2(largJogador, altJogador),
            new Vector2(0, 200),
            WHITE,
            TipoJogador.DOIS // Servidor gerencia o TipoJogador
        );
    }

    @Override
    public void update(double delta) {
        // Processa as entradas dos clientes que foram recebidas pela thread de rede.
        processarEntradasClientes();

        // Atualiza a lógica do jogo (bola e jogadores) no servidor.
        // O servidor é a autoridade.
        bola.atualizar(this, delta);
        // Os jogadores são atualizados com base nas entradas recebidas,
        // mas a lógica de movimento real (velocidade, limites) ainda pode ser controlada aqui
        // ou diretamente na classe Jogador se ela tiver métodos para isso.
        //jogador1.atualizar(this, delta); // Pode ser necessário ajustar o método atualizar do Jogador
        //jogador2.atualizar(this, delta); // para não mover o jogador automaticamente, apenas aplicar entrada.
    }

    @Override
    public void draw() {
        clearBackground(BLACK);
        bola.desenhar(this);
        jogador1.desenhar(this);
        jogador2.desenhar(this);
    }

    /**
     * Inicia o servidor UDP e as threads de rede.
     */
    private void iniciarServidor() {
        if (!executando) {
            executando = true;
            try {
                servidor = DatagramChannel.open();
                servidor.bind(new InetSocketAddress(porta));
                servidor.configureBlocking(false); // Canal não bloqueante

                // Inicializa o scheduler para gerenciar as threads de rede
                scheduler = Executors.newScheduledThreadPool(2); // Uma para receber, outra para enviar

                // Thread para receber dados dos clientes
                scheduler.scheduleAtFixedRate(this::receberDadosClientes, 0, 10, TimeUnit.MILLISECONDS);

                // Thread para enviar o estado do jogo para os clientes
                scheduler.scheduleAtFixedRate(this::enviarEstadoJogo, 0, UPDATE_RATE_MS, TimeUnit.MILLISECONDS);

                System.out.println("Servidor iniciado na porta " + porta);

            } catch (IOException exc) {
                System.err.println("Erro ao iniciar o servidor: " + exc.getMessage());
                exc.printStackTrace();
                executando = false;
            }
        }
    }

    /**
     * Processa as entradas dos clientes que foram enfileiradas pela thread de recebimento.
     * Este método é chamado no loop principal do jogo (update()).
     */
    private void processarEntradasClientes() {
        ClientInput input;
        while ((input = filaEntradasClientes.poll()) != null) {
            // Aqui você aplica a entrada do cliente ao jogador correspondente.
            // Exemplo: se o input contiver a nova posição Y do jogador.
            if (input.tipoJogador == TipoJogador.UM.valor) {
                jogador1.pos.y = input.y;
                jogador1.dim.x = input.largura; // Atualiza dimensões também se necessário
                jogador1.dim.y = input.altura;
            } else if (input.tipoJogador == TipoJogador.DOIS.valor) {
                jogador2.pos.y = input.y;
                jogador2.dim.x = input.largura;
                jogador2.dim.y = input.altura;
            }
            // Você também pode registrar o remetente para futuras comunicações
            if (!clientesConectados.containsKey(TipoJogador.fromValor(input.tipoJogador))) {
                clientesConectados.put(TipoJogador.fromValor(input.tipoJogador), input.remetente);
                System.out.println("Cliente " + TipoJogador.fromValor(input.tipoJogador) + " conectado de: " + input.remetente);
            }
        }
    }

    /**
     * Thread de recebimento: recebe dados dos clientes e os enfileira.
     */
    private void receberDadosClientes() {
        try {
            buffer.clear();
            SocketAddress remetente = servidor.receive(buffer);

            if (remetente != null) {
                buffer.flip();
                // Decodifica os dados binários
                // Formato: tipoJogador (int), x (double), y (double), largura (double), altura (double)
                if (buffer.remaining() >= (4 * 5)) { // 5 doubles = 20 bytes
                    int tipoJogador = buffer.getInt();
                    double x = buffer.getFloat();
                    double y = buffer.getFloat();
                    double largura = buffer.getFloat();
                    double altura = buffer.getFloat();

                    // Adiciona a entrada à fila para ser processada na thread principal do jogo
                    filaEntradasClientes.offer(new ClientInput(remetente, tipoJogador, x, y, largura, altura));
                }
            }
        } catch (IOException exc) {
            // Não imprima stack trace para cada erro de "nada para receber", pois é esperado.
            // Apenas para erros reais de I/O.
            // System.err.println("Erro ao receber dados (servidor): " + exc.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao processar dados recebidos (servidor): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Thread de envio: envia o estado atual do jogo para todos os clientes conectados.
     */
    private void enviarEstadoJogo() {
        if (servidor == null || clientesConectados.isEmpty()) {
            return;
        }

        // Prepara o buffer com o estado atual do jogo
        // Formato: bola.x, bola.y, bola.raio, jogador1.x, jogador1.y, jogador1.larg, jogador1.alt,
        //          jogador2.x, jogador2.y, jogador2.larg, jogador2.alt (todos doubles)
        ByteBuffer sendBuffer = ByteBuffer.allocate(4 * 11); // 11 doubles = 44 bytes

        sendBuffer.putDouble((double) bola.pos.x);
        sendBuffer.putDouble((double) bola.pos.y);
        sendBuffer.putDouble((double) bola.raio);

        sendBuffer.putDouble((double) jogador1.pos.x);
        sendBuffer.putDouble((double) jogador1.pos.y);
        sendBuffer.putDouble((double) jogador1.dim.x);
        sendBuffer.putDouble((double) jogador1.dim.y);

        sendBuffer.putDouble((double) jogador2.pos.x);
        sendBuffer.putDouble((double) jogador2.pos.y);
        sendBuffer.putDouble((double) jogador2.dim.x);
        sendBuffer.putDouble((double) jogador2.dim.y);

        sendBuffer.flip(); // Prepara o buffer para leitura

        // Envia o estado para cada cliente conectado
        for (Map.Entry<TipoJogador, SocketAddress> entry : clientesConectados.entrySet()) {
            try {
                // Cria uma cópia do buffer para cada envio, pois send() consome o buffer.
                ByteBuffer bufferCopy = sendBuffer.duplicate();
                servidor.send(bufferCopy, entry.getValue());
            } catch (IOException exc) {
                System.err.println("Erro ao enviar estado para " + entry.getValue() + ": " + exc.getMessage());
                // Remover cliente se a conexão falhar? Depende da robustez desejada.
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
        if (servidor != null) {
            try {
                servidor.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o servidor: " + e.getMessage());
            }
        }
    }

    /**
     * Classe interna para encapsular a entrada do cliente e o remetente.
     */
    private static class ClientInput {
        SocketAddress remetente;
        int tipoJogador;
        double x, y, largura, altura;

        public ClientInput(SocketAddress remetente, int tipoJogador, double x, double y, double largura, double altura) {
            this.remetente = remetente;
            this.tipoJogador = tipoJogador;
            this.x = x;
            this.y = y;
            this.largura = largura;
            this.altura = altura;
        }
    }
}
