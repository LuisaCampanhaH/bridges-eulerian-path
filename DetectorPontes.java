/*
 * Interface comum das duas estrategias de identificacao de pontes.
 * PontesNaive (Pessoa A) e PontesTarjan (Pessoa B) implementam estes metodos,
 * e o Fleury recebe um DetectorPontes sem precisar saber qual e.
 */
public interface DetectorPontes {

    // a aresta id (ainda nao removida) e ponte no grafo atual?
    boolean ehPonte(Grafo g, int id);

    // ids de todas as pontes do grafo atual
    int[] encontrarPontes(Grafo g);
}
