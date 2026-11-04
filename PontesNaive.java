import java.io.IOException;
import java.util.Scanner;

/*
 * Metodo naive: para cada aresta, remove, testa a conectividade e restaura.
 *
 * Uma aresta {u, v} e ponte se e somente se, depois de remove-la, nao existe
 * mais caminho entre u e v. Por isso o teste de conectividade aqui e
 * "u ainda alcanca v?", com uma BFS que para assim que encontra v.
 *
 * Custo: O(V + E) por aresta testada, ou seja O(E * (V + E)) para todas.
 */
public class PontesNaive implements DetectorPontes {

    public boolean ehPonte(Grafo g, int id) {
        if (g.estaRemovida(id)) {
            return false;
        }
        int u = g.extremidadeU[id];
        int v = g.extremidadeV[id];

        g.removerAresta(id);
        boolean aindaLigados = g.alcanca(u, v);
        g.restaurarAresta(id);

        return !aindaLigados;
    }

    public int[] encontrarPontes(Grafo g) {
        int[] pontes = new int[g.m];
        int quantidade = 0;

        for (int id = 0; id < g.m; id++) {
            if (!g.estaRemovida(id) && ehPonte(g, id)) {
                pontes[quantidade] = id;
                quantidade++;
            }
        }

        // copia para um vetor do tamanho exato
        int[] resultado = new int[quantidade];
        for (int i = 0; i < quantidade; i++) {
            resultado[i] = pontes[i];
        }
        return resultado;
    }

    // teste da parte da Pessoa A: le um grafo e lista as pontes
    public static void main(String[] args) throws IOException {
        Scanner teclado = new Scanner(System.in);

        System.out.print("Nome do arquivo do grafo: ");
        String nomeArquivo = teclado.nextLine().trim();

        Grafo g = Grafo.lerArquivo(nomeArquivo);
        System.out.println("Vertices: " + g.n + " | Arestas: " + g.m);
        System.out.println("Conexo: " + (g.estaConexo() ? "sim" : "nao"));

        PontesNaive naive = new PontesNaive();

        long tempoInicio = System.nanoTime();
        int[] pontes = naive.encontrarPontes(g);
        long tempoFim = System.nanoTime();

        System.out.println("\nPontes encontradas (" + pontes.length + "):");
        for (int i = 0; i < pontes.length; i++) {
            int id = pontes[i];
            System.out.println(g.extremidadeU[id] + " - " + g.extremidadeV[id]);
        }
        System.out.printf("Tempo de execucao: %.3f ms%n", (tempoFim - tempoInicio) / 1000000.0);

        teclado.close();
    }
}
