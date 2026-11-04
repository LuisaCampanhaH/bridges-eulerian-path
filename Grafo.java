import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/*
 * Grafo simples nao-direcionado, com vertices numerados de 1 a n.
 *
 * Representacao: "vetor de inicios" (CSR), o mesmo esquema dos trabalhos
 * anteriores. Como o grafo e nao-direcionado, cada aresta {u, v} aparece
 * duas vezes nas listas: v na lista de u e u na lista de v.
 *
 * Cada aresta tem um identificador (id) de 0 a m-1. Para remover uma aresta
 * basta marcar removida[id] = true, em O(1), sem mexer nos vetores. Quem
 * percorre as listas simplesmente pula as posicoes cuja aresta foi removida.
 * Isso e importante porque o Fleury remove arestas o tempo todo e o metodo
 * naive remove e restaura uma aresta para cada teste.
 *
 * Para percorrer os vizinhos ativos de v:
 *
 *     for (int i = g.inicio[v]; i < g.inicio[v + 1]; i++) {
 *         int id = g.arestaDaPosicao[i];
 *         if (g.removida[id]) continue;
 *         int w = g.vizinho[i];
 *         ...
 *     }
 */
public class Grafo {

    int n, m;

    // os dois vertices de cada aresta: a aresta id liga extremidadeU[id] a extremidadeV[id]
    int[] extremidadeU;
    int[] extremidadeV;

    // listas de adjacencia (CSR): os vizinhos de v ficam nas posicoes [inicio[v], inicio[v + 1])
    int[] inicio;
    int[] vizinho;          // vizinho[i]: vertice da posicao i
    int[] arestaDaPosicao;  // arestaDaPosicao[i]: id da aresta que gerou a posicao i

    boolean[] removida;
    int[] grau;             // grau atual, ja descontando as arestas removidas
    int arestasAtivas;

    // vetores auxiliares da busca em largura, alocados uma unica vez
    int[] fila;
    int[] marca;
    int marcaAtual;

    public Grafo(int n, int[] origemAresta, int[] destinoAresta) {
        this.n = n;
        this.m = origemAresta.length;

        extremidadeU = new int[m];
        extremidadeV = new int[m];
        for (int id = 0; id < m; id++) {
            extremidadeU[id] = origemAresta[id];
            extremidadeV[id] = destinoAresta[id];
        }

        montarListas();

        removida = new boolean[m];
        arestasAtivas = m;

        fila = new int[n + 1];
        marca = new int[n + 1];
        marcaAtual = 0;
    }

    public static Grafo lerArquivo(String nomeArquivo) throws IOException {
        BufferedReader leitor = new BufferedReader(new FileReader(nomeArquivo));

        StringTokenizer primeiraLinha = new StringTokenizer(leitor.readLine());
        int n = Integer.parseInt(primeiraLinha.nextToken());
        int m = Integer.parseInt(primeiraLinha.nextToken());

        int[] origemAresta = new int[m];
        int[] destinoAresta = new int[m];

        for (int i = 0; i < m; i++) {
            StringTokenizer linha = new StringTokenizer(leitor.readLine());
            origemAresta[i] = Integer.parseInt(linha.nextToken());
            destinoAresta[i] = Integer.parseInt(linha.nextToken());
        }
        leitor.close();

        return new Grafo(n, origemAresta, destinoAresta);
    }

    /*
     * Monta o CSR em O(n + m) com a ideia do counting sort:
     * 1) conta o grau de cada vertice;
     * 2) soma acumulada dos graus = posicao onde comeca a lista de cada vertice;
     * 3) percorre as arestas escrevendo cada uma nas duas listas.
     */
    private void montarListas() {
        grau = new int[n + 1];
        for (int id = 0; id < m; id++) {
            grau[extremidadeU[id]]++;
            grau[extremidadeV[id]]++;
        }

        inicio = new int[n + 2];
        inicio[1] = 0;
        for (int v = 1; v <= n; v++) {
            inicio[v + 1] = inicio[v] + grau[v];
        }

        vizinho = new int[2 * m];
        arestaDaPosicao = new int[2 * m];

        // posicao de escrita atual de cada vertice, comecando no inicio do seu bloco
        int[] posicao = new int[n + 2];
        for (int v = 1; v <= n + 1; v++) {
            posicao[v] = inicio[v];
        }

        for (int id = 0; id < m; id++) {
            int u = extremidadeU[id];
            int v = extremidadeV[id];

            vizinho[posicao[u]] = v;
            arestaDaPosicao[posicao[u]] = id;
            posicao[u]++;

            vizinho[posicao[v]] = u;
            arestaDaPosicao[posicao[v]] = id;
            posicao[v]++;
        }
    }

    public int grau(int v) {
        return grau[v];
    }

    public boolean estaRemovida(int id) {
        return removida[id];
    }

    // dado um dos lados da aresta, devolve o outro
    public int outroLado(int id, int v) {
        if (extremidadeU[id] == v) {
            return extremidadeV[id];
        }
        return extremidadeU[id];
    }

    public void removerAresta(int id) {
        if (removida[id]) {
            return;
        }
        removida[id] = true;
        grau[extremidadeU[id]]--;
        grau[extremidadeV[id]]--;
        arestasAtivas--;
    }

    public void restaurarAresta(int id) {
        if (!removida[id]) {
            return;
        }
        removida[id] = false;
        grau[extremidadeU[id]]++;
        grau[extremidadeV[id]]++;
        arestasAtivas++;
    }

    /*
     * Em vez de zerar um vetor "visitado" a cada busca (O(n) toda vez),
     * cada busca usa um numero novo. Um vertice esta visitado na busca atual
     * se marca[v] == marcaAtual. Assim, uma busca que termina cedo custa
     * so o que ela realmente percorreu.
     */
    private void novaMarca() {
        marcaAtual++;
        if (marcaAtual == Integer.MAX_VALUE) {
            for (int v = 0; v <= n; v++) {
                marca[v] = 0;
            }
            marcaAtual = 1;
        }
    }

    /*
     * Busca em largura (BFS) a partir de origem, ignorando arestas removidas.
     * Se alvo != 0, para assim que encontrar o alvo.
     * Devolve quantos vertices foram visitados.
     * E iterativa (fila propria) para nao estourar a pilha com 100.000 vertices.
     */
    private int buscaLargura(int origem, int alvo) {
        novaMarca();

        int comecoFila = 0, fimFila = 0;
        fila[fimFila] = origem;
        fimFila++;
        marca[origem] = marcaAtual;
        int visitados = 1;

        if (origem == alvo) {
            return visitados;
        }

        while (comecoFila < fimFila) {
            int v = fila[comecoFila];
            comecoFila++;

            for (int i = inicio[v]; i < inicio[v + 1]; i++) {
                if (removida[arestaDaPosicao[i]]) {
                    continue;
                }
                int w = vizinho[i];
                if (marca[w] != marcaAtual) {
                    marca[w] = marcaAtual;
                    visitados++;
                    if (w == alvo) {
                        return visitados;
                    }
                    fila[fimFila] = w;
                    fimFila++;
                }
            }
        }
        return visitados;
    }

    // existe caminho de origem ate alvo usando so arestas nao removidas?
    public boolean alcanca(int origem, int alvo) {
        buscaLargura(origem, alvo);
        return marca[alvo] == marcaAtual;
    }

    /*
     * O grafo e conexo? Vertices isolados (grau 0) sao ignorados, porque
     * para caminho euleriano so importam os vertices que ainda tem arestas.
     */
    public boolean estaConexo() {
        int primeiro = 0;
        int comAresta = 0;
        for (int v = 1; v <= n; v++) {
            if (grau[v] > 0) {
                comAresta++;
                if (primeiro == 0) {
                    primeiro = v;
                }
            }
        }

        if (comAresta == 0) {
            return true;
        }
        return buscaLargura(primeiro, 0) == comAresta;
    }
}
