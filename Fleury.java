import java.io.IOException;
import java.util.Scanner;

/*
 * Verificacao euleriana e caminho euleriano pelo metodo de Fleury.
 * A cada passo anda por uma aresta que nao seja ponte, a menos que seja a unica opcao.
 * Recebe um DetectorPontes, entao roda com o naive e com o Tarjan sem mudar nada aqui.
 */
public class Fleury {

    public static final int NAO_EULERIANO = 0;
    public static final int SEMI_EULERIANO = 1;
    public static final int EULERIANO = 2;

    private static int contarImpares(Grafo g){
        int impares = 0;
        for(int v = 1; v <= g.n; v++){
            if(g.grau(v) % 2 != 0){ impares++; }
        }
        return impares;
    }

    public static int classificar(Grafo g){
        if(!g.estaConexo()){
            return NAO_EULERIANO;
        }

        int impares = contarImpares(g);
        if(impares == 0){
            return EULERIANO;
        } else if(impares == 2){
            return SEMI_EULERIANO;
        }
        return NAO_EULERIANO;
    }

    // semi-euleriano precisa comecar em um vertice impar; euleriano pode comecar em qualquer um com aresta
    private static int escolherInicio(Grafo g){
        int comAresta = 0;
        for(int v = 1; v <= g.n; v++){
            if(g.grau(v) % 2 != 0){
                return v;
            }
            if(comAresta == 0 && g.grau(v) > 0){
                comAresta = v;
            }
        }

        if(comAresta == 0){
            return 1;
        }
        return comAresta;
    }

    private static int escolherAresta(Grafo g, DetectorPontes detector, int atual, int[] cursor){
        int fim = g.inicio[atual + 1];

        // aresta removida nao volta durante o Fleury, entao o cursor nunca precisa recuar
        while(g.removida[g.arestaDaPosicao[cursor[atual]]]){
            cursor[atual]++;
        }

        int primeira = g.arestaDaPosicao[cursor[atual]];
        if(g.grau(atual) == 1){
            return primeira;
        }

        for(int i = cursor[atual]; i < fim; i++){
            int id = g.arestaDaPosicao[i];
            if(g.removida[id]){ continue; }
            if(!detector.ehPonte(g, id)){
                return id;
            }
        }
        return primeira;
    }

    // devolve os vertices do caminho, ou null se ele nao existir. O grafo volta ao estado original no fim.
    public static int[] encontrarCaminho(Grafo g, DetectorPontes detector){
        if(classificar(g) == NAO_EULERIANO){
            return null;
        }

        int total = g.arestasAtivas;
        int[] caminho = new int[total + 1];
        int[] usadas = new int[total];
        int[] cursor = new int[g.n + 2];

        for(int v = 1; v <= g.n; v++){
            cursor[v] = g.inicio[v];
        }

        int atual = escolherInicio(g);
        caminho[0] = atual;

        for(int passo = 0; passo < total; passo++){
            int id = escolherAresta(g, detector, atual, cursor);
            atual = g.outroLado(id, atual);
            g.removerAresta(id);
            usadas[passo] = id;
            caminho[passo + 1] = atual;
        }

        for(int passo = 0; passo < total; passo++){
            g.restaurarAresta(usadas[passo]);
        }
        return caminho;
    }

    private static void imprimirCaminho(int[] caminho){
        StringBuilder saida = new StringBuilder("[");
        for(int i = 0; i < caminho.length; i++){
            saida.append(caminho[i]);
            if(i != caminho.length - 1){ saida.append(", "); }
        }
        saida.append("]");
        System.out.println(saida);
    }

    public static void main(String[] args) throws IOException {
        Scanner teclado = new Scanner(System.in);

        System.out.print("Digite o nome do arquivo do grafo: ");
        String nomeArquivo = teclado.nextLine().trim();

        System.out.println("Digite qual estrategia voce deseja usar:");
        System.out.println("Digite [1]: naive");
        System.out.println("Digite [2]: Tarjan");
        int estrategia = teclado.nextInt();
        teclado.close();

        DetectorPontes detector;
        if(estrategia == 1){
            detector = new PontesNaive();
        } else if(estrategia == 2){
            detector = new PontesTarjan();
        } else {
            System.out.println("Numero errado, tente novamente");
            return;
        }

        Grafo g = Grafo.lerArquivo(nomeArquivo);
        System.out.printf("O Grafo escolhido possui %d vertices e %d arestas\n", g.n, g.m);

        int tipo = classificar(g);
        if(tipo == EULERIANO){
            System.out.println("Tipo: euleriano");
        } else if(tipo == SEMI_EULERIANO){
            System.out.println("Tipo: semi-euleriano");
        } else {
            System.out.println("Tipo: nao euleriano");
        }

        long comeco = System.nanoTime();
        int[] caminho = encontrarCaminho(g, detector);
        long fim = System.nanoTime();

        if(caminho == null){
            System.out.println("Nao existe caminho euleriano");
        } else {
            System.out.print("Caminho euleriano: ");
            imprimirCaminho(caminho);
        }
        System.out.printf("Tempo de execucao: %.3f ms\n", (fim - comeco) / 1000000.0);
    }
}
