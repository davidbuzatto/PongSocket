package pong.gem;

import pong.componentes.TipoJogador;

/**
 * Classe principal para iniciar o servidor e os clientes do jogo Pong.
 *
 * @author Prof. Dr. David Buzatto (Adaptado por Gemini) - não funciona :D:D:D
 */
public class Pong {

    public static void main(String[] args) {

        int porta = 8080;
        int tamanhoBuffer = 1024; // Tamanho do buffer para comunicação
        String host = "localhost"; // Endereço do servidor

        // Cria e inicia o servidor
        Servidor s = new Servidor(porta, tamanhoBuffer);

        // Cria e inicia os clientes
        // Eles se conectarão ao servidor na porta e host especificados
        Cliente c1 = new Cliente(porta, host, tamanhoBuffer, TipoJogador.UM);
        Cliente c2 = new Cliente(porta, host, tamanhoBuffer, TipoJogador.DOIS);

        // Ajusta a localização das janelas para melhor visualização (opcional)
        s.setLocation(s.getLocation().x, s.getLocation().y - s.getSize().height / 2);
        c1.setLocation(c1.getLocation().x - c1.getSize().width / 2, c1.getLocation().y + c1.getSize().height / 2);
        c2.setLocation(c2.getLocation().x + c2.getSize().width / 2, c2.getLocation().y + c2.getSize().height / 2);

        // Nota: A inicialização das threads de rede ocorre dentro dos construtores
        // do Servidor e Cliente, garantindo que a comunicação comece junto com as janelas.
    }
}
