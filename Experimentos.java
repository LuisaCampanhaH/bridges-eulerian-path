import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/*
 * Experimentos de tempo das duas estrategias.
 *
 *   java Experimentos [tamanhos] [timeout_s]   roda tudo e gera os CSVs
 *       ex.: java Experimentos 100,1000 60     (padrao: 100,1000,10000,100000 e 1800 s)
 *   java Experimentos resumir                  so refaz os resumos a partir de resultados/bruto.csv
 *
 * Duas medidas:
 *   fleury: Fleury.encontrarCaminho nos tres tipos de grafo;
 *   pontes: encontrarPontes sozinho (todas as pontes), em grafos nao eulerianos.
 *
 * Cada execucao roda em um processo Java separado (modo "rodar"). Assim o timeout
 * e so matar o processo, sem mexer no Fleury, e uma execucao nao aquece o JIT
 * nem suja a memoria da seguinte. A geracao do grafo fica fora da medicao, e a
 * mesma semente faz as duas estrategias rodarem sobre o mesmo grafo.
 *
 * Saida:
 *   resultados/bruto.csv            uma linha por execucao
 *   resultados/<medida>_<tipo>.csv  medias por n, lado a lado (lido pelo pgfplots no relatorio)
 */
public class Experimentos {

    // indice = constante do Fleury (NAO_EULERIANO = 0, SEMI_EULERIANO = 1, EULERIANO = 2)
    static final String[] NOME_TIPO = {"nao", "semi", "euleriano"};
    static final String[] ESTRATEGIAS = {"naive", "tarjan"};
    static final String PASTA = "resultados";
    static final String BRUTO = PASTA + "/bruto.csv";

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT); // ponto decimal nos CSVs

        if (args.length > 0 && args[0].equals("rodar")) {
            rodar(args);
            return;
        }
        if (args.length > 0 && args[0].equals("resumir")) {
            resumir();
            return;
        }

        int[] tamanhos = {100, 1000, 10000, 100000};
        if (args.length > 0) {
            String[] partes = args[0].split(",");
            tamanhos = new int[partes.length];
            for (int i = 0; i < partes.length; i++) {
                tamanhos[i] = Integer.parseInt(partes[i].trim());
            }
        }
        long timeout = args.length > 1 ? Long.parseLong(args[1]) : 1800;

        executarTodos(tamanhos, timeout);
        resumir();
    }

    static int repeticoes(int n) {
        if (n <= 1000) return 10;
        if (n <= 10000) return 5;
        return 3;
    }

    // ---------------------------------------------------------------- processo pai

    static void executarTodos(int[] tamanhos, long timeout) throws Exception {
        new File(PASTA).mkdirs();
        PrintWriter bruto = new PrintWriter(new FileWriter(BRUTO));
        bruto.println("medida,n,m,tipo,estrategia,repeticao,semente,tempo_ms,status");
        bruto.flush();

        for (int n : tamanhos) {
            for (String medida : new String[]{"fleury", "pontes"}) {
                // pontes nao depende do tipo, entao basta o grafo "qualquer" (nao euleriano)
                int[] tipos = medida.equals("fleury") ? new int[]{2, 1, 0} : new int[]{0};
                for (int tipo : tipos) {
                    for (int rep = 1; rep <= repeticoes(n); rep++) {
                        long semente = 1000L * n + 10L * tipo + rep;
                        for (String estrategia : ESTRATEGIAS) {
                            String linha = executarUma(medida, n, tipo, estrategia, rep, semente, timeout);
                            bruto.println(linha);
                            bruto.flush(); // se cair no meio, o que ja rodou fica salvo
                            System.out.println(linha);
                        }
                    }
                }
            }
        }
        bruto.close();
    }

    static String executarUma(String medida, int n, int tipo, String estrategia, int rep, long semente,
                              long timeout) throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        ProcessBuilder construtor = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                "Experimentos", "rodar", medida, "" + n, "" + tipo, estrategia, "" + semente);
        // saida do filho vai para um arquivo: continua legivel mesmo depois de matar o processo
        File arquivoSaida = File.createTempFile("experimento", ".txt");
        construtor.redirectErrorStream(true);
        construtor.redirectOutput(arquivoSaida);
        Process processo = construtor.start();

        String status;
        if (processo.waitFor(timeout, TimeUnit.SECONDS)) {
            status = processo.exitValue() == 0 ? "ok" : "erro";
        } else {
            processo.destroyForcibly().waitFor();
            status = "timeout";
        }

        // o filho imprime "m <m>" antes de medir e "tempo <ms> <ok|erro>" no fim
        String m = "", tempo = "";
        BufferedReader saida = new BufferedReader(new FileReader(arquivoSaida));
        for (String linha = saida.readLine(); linha != null; linha = saida.readLine()) {
            String[] partes = linha.split(" ");
            if (partes[0].equals("m")) {
                m = partes[1];
            } else if (partes[0].equals("tempo") && status.equals("ok")) {
                tempo = partes[1];
                status = partes[2];
            } else {
                System.err.println("  [filho] " + linha); // excecao ou erro do processo filho
            }
        }
        saida.close();
        arquivoSaida.delete();

        return medida + "," + n + "," + m + "," + NOME_TIPO[tipo] + "," + estrategia + "," + rep + ","
                + semente + "," + tempo + "," + status;
    }

    // ---------------------------------------------------------------- processo filho

    // argumentos: rodar <medida> <n> <tipo> <estrategia> <semente>
    static void rodar(String[] args) {
        String medida = args[1];
        int n = Integer.parseInt(args[2]);
        int tipo = Integer.parseInt(args[3]);
        DetectorPontes detector = args[4].equals("naive") ? new PontesNaive() : new PontesTarjan();
        long semente = Long.parseLong(args[5]);

        Grafo g = GeradorGrafos.gerar(n, tipo, semente);
        System.out.println("m " + g.m);
        System.out.flush();

        // grafos pequenos rodam rapido demais: uma execucao descartada aquece o JIT
        if (n <= 1000) {
            executar(medida, g, tipo, detector);
        }

        long comeco = System.nanoTime();
        boolean correto = executar(medida, g, tipo, detector);
        long fim = System.nanoTime();

        System.out.printf("tempo %.3f %s%n", (fim - comeco) / 1e6, correto ? "ok" : "erro");
    }

    // roda a medida e confere o resultado (o Fleury devolve o grafo ao estado original)
    static boolean executar(String medida, Grafo g, int tipo, DetectorPontes detector) {
        if (medida.equals("pontes")) {
            detector.encontrarPontes(g);
            return true;
        }
        int[] caminho = Fleury.encontrarCaminho(g, detector);
        if (tipo == Fleury.NAO_EULERIANO) {
            return caminho == null;
        }
        return caminho != null && caminho.length == g.m + 1;
    }

    // ---------------------------------------------------------------- resumo

    static class Celula {
        ArrayList<Double> tempos = new ArrayList<>();
        int timeouts;
        int erros;
        long somaM;
        int quantidadeM;
    }

    static void resumir() throws IOException {
        // "medida_tipo" -> n -> {naive, tarjan}
        Map<String, TreeMap<Integer, Celula[]>> grupos = new LinkedHashMap<>();

        BufferedReader leitor = new BufferedReader(new FileReader(BRUTO));
        leitor.readLine(); // cabecalho
        for (String linha = leitor.readLine(); linha != null; linha = leitor.readLine()) {
            String[] c = linha.split(",", -1);
            String medida = c[0], tipo = c[3], estrategia = c[4], status = c[8];
            int n = Integer.parseInt(c[1]);

            Celula celula = grupos.computeIfAbsent(medida + "_" + tipo, k -> new TreeMap<>())
                    .computeIfAbsent(n, k -> new Celula[]{new Celula(), new Celula()})
                    [estrategia.equals("naive") ? 0 : 1];

            if (!c[2].isEmpty()) {
                celula.somaM += Long.parseLong(c[2]);
                celula.quantidadeM++;
            }
            if (status.equals("ok")) {
                celula.tempos.add(Double.parseDouble(c[7]));
            } else if (status.equals("timeout")) {
                celula.timeouts++;
            } else {
                celula.erros++;
            }
        }
        leitor.close();

        for (Map.Entry<String, TreeMap<Integer, Celula[]>> grupo : grupos.entrySet()) {
            String arquivo = PASTA + "/" + grupo.getKey() + ".csv";
            PrintWriter saida = new PrintWriter(new FileWriter(arquivo));
            saida.println("n,m,naive_ms,naive_desvio,naive_ok,naive_timeouts,naive_erros,"
                    + "tarjan_ms,tarjan_desvio,tarjan_ok,tarjan_timeouts,tarjan_erros");

            for (Map.Entry<Integer, Celula[]> linha : grupo.getValue().entrySet()) {
                Celula naive = linha.getValue()[0], tarjan = linha.getValue()[1];
                long somaM = naive.somaM + tarjan.somaM;
                int quantidadeM = naive.quantidadeM + tarjan.quantidadeM;
                long m = quantidadeM == 0 ? 0 : Math.round((double) somaM / quantidadeM);
                saida.println(linha.getKey() + "," + m + "," + estatisticas(naive) + "," + estatisticas(tarjan));
            }
            saida.close();
            System.out.println("Resumo salvo em " + arquivo);
        }
    }

    // media e desvio padrao amostral das execucoes que terminaram; "nan" se nenhuma terminou
    static String estatisticas(Celula c) {
        int k = c.tempos.size();
        String media = "nan", desvio = "nan";
        if (k > 0) {
            double soma = 0;
            for (double t : c.tempos) soma += t;
            double mediaValor = soma / k;

            double quadrados = 0;
            for (double t : c.tempos) quadrados += (t - mediaValor) * (t - mediaValor);
            double desvioValor = k > 1 ? Math.sqrt(quadrados / (k - 1)) : 0;

            media = String.format("%.3f", mediaValor);
            desvio = String.format("%.3f", desvioValor);
        }
        return media + "," + desvio + "," + k + "," + c.timeouts + "," + c.erros;
    }
}
