import java.io.IOException;
import java.util.Scanner;

/*
 * Tarjan (1974): uma DFS so, guardando o tempo de descoberta e o low de cada vertice.
 * A aresta u-v da arvore da DFS e ponte quando low[v] > descoberta[u].
 * A pilha e um vetor nosso porque a recursao estoura com 100.000 vertices.
 */
public class PontesTarjan implements DetectorPontes {

    private int[] descoberta;
    private int[] low;
    private int[] arestaDoPai;
    private int[] cursor;
    private int[] pilha;
    private int[] marca;
    private int marcaAtual;
    private int tempo;

    private int[] pontes;
    private int quantidade;

    private void prepararBusca(Grafo g){
        if(descoberta == null || descoberta.length != g.n + 1){
            descoberta = new int[g.n + 1];
            low = new int[g.n + 1];
            arestaDoPai = new int[g.n + 1];
            cursor = new int[g.n + 1];
            pilha = new int[g.n + 1];
            marca = new int[g.n + 1];
            pontes = new int[g.n + 1];
            marcaAtual = 0;
        }
        marcaAtual++;
        tempo = 0;
        quantidade = 0;
    }

    private void descobrir(Grafo g, int v, int idPai){
        tempo++;
        marca[v] = marcaAtual;
        descoberta[v] = tempo;
        low[v] = tempo;
        arestaDoPai[v] = idPai;
        cursor[v] = g.inicio[v];
    }

    private void buscaProfundidade(Grafo g, int raiz){
        int topo = 0;
        pilha[topo++] = raiz;
        descobrir(g, raiz, -1);

        while(topo > 0){
            int v = pilha[topo - 1];

            if(cursor[v] < g.inicio[v + 1]){
                int i = cursor[v]++;
                int id = g.arestaDaPosicao[i];
                if(g.removida[id] || id == arestaDoPai[v]){ continue; }

                int w = g.vizinho[i];
                if(marca[w] != marcaAtual){
                    descobrir(g, w, id);
                    pilha[topo++] = w;
                } else if(descoberta[w] < low[v]){
                    low[v] = descoberta[w];
                }
            } else {
                // acabaram os vizinhos de v: agora o low dele esta pronto e sobe para o pai
                topo--;
                if(topo > 0){
                    int pai = pilha[topo - 1];
                    if(low[v] < low[pai]){ low[pai] = low[v]; }
                    if(low[v] > descoberta[pai]){
                        pontes[quantidade++] = arestaDoPai[v];
                    }
                }
            }
        }
    }

    // o grafo muda a cada passo do Fleury, entao cada pergunta faz uma DFS nova a partir de u
    public boolean ehPonte(Grafo g, int id){
        if(g.estaRemovida(id)){
            return false;
        }
        int u = g.extremidadeU[id];
        int v = g.extremidadeV[id];

        prepararBusca(g);
        buscaProfundidade(g, u);

        // se v nao foi descoberto por essa aresta, ela fechou um ciclo e nao e ponte
        return arestaDoPai[v] == id && low[v] > descoberta[u];
    }

    public int[] encontrarPontes(Grafo g){
        prepararBusca(g);

        for(int v = 1; v <= g.n; v++){
            if(marca[v] != marcaAtual && g.grau(v) > 0){
                buscaProfundidade(g, v);
            }
        }

        int[] resultado = new int[quantidade];
        for(int i = 0; i < quantidade; i++){
            resultado[i] = pontes[i];
        }
        return resultado;
    }

    public static void main(String[] args) throws IOException {
        Scanner teclado = new Scanner(System.in);

        System.out.print("Digite o nome do arquivo do grafo: ");
        String nomeArquivo = teclado.nextLine().trim();
        teclado.close();

        Grafo g = Grafo.lerArquivo(nomeArquivo);
        System.out.printf("O Grafo escolhido possui %d vertices e %d arestas\n", g.n, g.m);

        PontesTarjan tarjan = new PontesTarjan();

        long comeco = System.nanoTime();
        int[] pontes = tarjan.encontrarPontes(g);
        long fim = System.nanoTime();

        System.out.printf("\nPontes encontradas: %d\n", pontes.length);
        if(pontes.length == 0){
            System.out.println("Nenhuma");
        }
        for(int i = 0; i < pontes.length; i++){
            System.out.println(g.extremidadeU[pontes[i]] + " - " + g.extremidadeV[pontes[i]]);
        }
        System.out.printf("Tempo de execucao: %.3f ms\n", (fim - comeco) / 1000000.0);
    }
}
