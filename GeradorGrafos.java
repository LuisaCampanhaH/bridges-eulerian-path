import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

/*
 * Gerador de grafos aleatorios conexos, simples e esparsos (grau medio ~4)
 * nos tres tipos do trabalho: euleriano, semi-euleriano e nao euleriano.
 *
 * 1) Arvore aleatoria: cada vertice i >= 2 liga em um j < i sorteado. Isso ja
 *    deixa o grafo conexo.
 * 2) Arestas aleatorias ate ~1,75n. O ajuste de paridade acrescenta mais ~n/4,
 *    fechando em ~2n arestas, ou seja, grau medio ~4.
 * 3) Ajuste de paridade so ADICIONANDO arestas, entao o grafo continua conexo:
 *    os vertices impares sao ligados em pares (u, v). Se u-v ja existe, liga
 *    u-w-v com um w sorteado (w ganha 2 de grau e nao muda de paridade).
 *    Sobram 0 impares (euleriano), 2 (semi-euleriano) ou 4 (nao euleriano).
 *
 * Um HashSet com o par (menor, maior) impede lacos e arestas repetidas.
 * Mesma semente gera o mesmo grafo, entao as duas estrategias rodam sobre o mesmo grafo.
 */
public class GeradorGrafos {

    private final int n;
    private final Random sorteio;
    private final HashSet<Long> existentes = new HashSet<>();
    private final int[] origem, destino, grau;
    private int m;

    private GeradorGrafos(int n, long semente) {
        this.n = n;
        this.sorteio = new Random(semente);
        // arvore (n) + aleatorias (1,75n) + no maximo 2 arestas por par de impares (n)
        origem = new int[4 * n];
        destino = new int[4 * n];
        grau = new int[n + 1];
    }

    // tipo: Fleury.EULERIANO, Fleury.SEMI_EULERIANO ou Fleury.NAO_EULERIANO
    public static Grafo gerar(int n, int tipo, long semente) {
        if (n < 8) {
            throw new IllegalArgumentException("o gerador precisa de pelo menos 8 vertices");
        }
        GeradorGrafos gerador = new GeradorGrafos(n, semente);
        gerador.montarBase();
        gerador.ajustarParidade(tipo);
        return new Grafo(n, Arrays.copyOf(gerador.origem, gerador.m), Arrays.copyOf(gerador.destino, gerador.m));
    }

    private long chave(int u, int v) {
        return u < v ? (long) u * (n + 1) + v : (long) v * (n + 1) + u;
    }

    private boolean existe(int u, int v) {
        return existentes.contains(chave(u, v));
    }

    // adiciona u-v se nao for laco nem repetida; devolve se adicionou
    private boolean adicionar(int u, int v) {
        if (u == v || !existentes.add(chave(u, v))) {
            return false;
        }
        origem[m] = u;
        destino[m] = v;
        m++;
        grau[u]++;
        grau[v]++;
        return true;
    }

    private int sortearVertice() {
        return 1 + sorteio.nextInt(n);
    }

    private void montarBase() {
        for (int v = 2; v <= n; v++) {
            adicionar(v, 1 + sorteio.nextInt(v - 1));
        }
        int alvo = 7 * n / 4;
        while (m < alvo) {
            adicionar(sortearVertice(), sortearVertice());
        }
    }

    // troca a paridade de u e de v sem mexer na de mais ninguem
    private void ligarPar(int u, int v) {
        if (adicionar(u, v)) {
            return;
        }
        while (true) {
            int w = sortearVertice();
            if (w != u && w != v && !existe(u, w) && !existe(w, v)) {
                adicionar(u, w);
                adicionar(w, v);
                return;
            }
        }
    }

    private ArrayList<Integer> verticesImpares() {
        ArrayList<Integer> impares = new ArrayList<>();
        for (int v = 1; v <= n; v++) {
            if (grau[v] % 2 != 0) {
                impares.add(v);
            }
        }
        return impares;
    }

    private void ajustarParidade(int tipo) {
        int manter = 0;
        if (tipo == Fleury.SEMI_EULERIANO) {
            manter = 2;
        } else if (tipo == Fleury.NAO_EULERIANO) {
            manter = 4;
        }

        // poucos impares (raro): uma aresta entre dois vertices pares cria mais dois
        int quantidade = verticesImpares().size();
        while (quantidade < manter) {
            int u = sortearVertice();
            int v = sortearVertice();
            if (grau[u] % 2 == 0 && grau[v] % 2 == 0 && adicionar(u, v)) {
                quantidade += 2;
            }
        }

        // liga os impares dois a dois, deixando so 'manter' deles impares
        ArrayList<Integer> impares = verticesImpares();
        Collections.shuffle(impares, sorteio);
        for (int i = manter; i + 1 < impares.size(); i += 2) {
            ligarPar(impares.get(i), impares.get(i + 1));
        }
    }

    // autoteste: gera os tres tipos em cada tamanho e confere tipo, conexidade e se e simples
    public static void main(String[] args) {
        int[] tamanhos = {100, 1000, 10000, 100000};
        String[] nomes = {"nao euleriano", "semi-euleriano", "euleriano"};

        for (int n : tamanhos) {
            for (int tipo = 0; tipo < 3; tipo++) {
                long comeco = System.nanoTime();
                Grafo g = gerar(n, tipo, 12345 + n + tipo);
                long fim = System.nanoTime();

                HashSet<Long> vistas = new HashSet<>();
                for (int id = 0; id < g.m; id++) {
                    int u = g.extremidadeU[id], v = g.extremidadeV[id];
                    long chave = Math.min(u, v) * (long) (n + 1) + Math.max(u, v);
                    if (u == v || !vistas.add(chave)) {
                        throw new AssertionError("grafo nao e simples");
                    }
                }
                // estaConexo ignora vertices isolados, entao confere tambem que nao ha nenhum
                for (int v = 1; v <= n; v++) {
                    if (g.grau(v) == 0) {
                        throw new AssertionError("vertice isolado");
                    }
                }
                if (!g.estaConexo()) {
                    throw new AssertionError("grafo desconexo");
                }
                if (Fleury.classificar(g) != tipo) {
                    throw new AssertionError("tipo errado para " + nomes[tipo]);
                }

                System.out.printf("n=%-6d m=%-6d grau medio=%.2f  %-15s %.1f ms%n",
                        n, g.m, 2.0 * g.m / n, nomes[tipo], (fim - comeco) / 1e6);
            }
        }
        System.out.println("Autoteste OK");
    }
}
