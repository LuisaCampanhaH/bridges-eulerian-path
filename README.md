# 🌉 TP01: Pontes e Caminhos Eulerianos

Trabalho Prático 01 da disciplina **Teoria dos Grafos e Computabilidade**
PUC Minas · Ciência da Computação · Prof. Zenilton Kleber Gonçalves do Patrocínio Júnior

Implementação e comparação de **duas estratégias para encontrar pontes** em grafos simples não-direcionados (o método **naïve** e o método de **Tarjan, 1974**), usadas dentro do **algoritmo de Fleury** para encontrar caminhos eulerianos.

---

## 📌 Objetivo

Uma **ponte** é uma aresta cuja remoção desconecta o grafo. O algoritmo de Fleury monta um caminho euleriano escolhendo, a cada passo, uma aresta que **não** seja ponte (a menos que não haja outra opção). Por isso, a velocidade do Fleury depende diretamente de quão rápido conseguimos responder à pergunta *"essa aresta é ponte?"*.

O trabalho compara as duas estratégias:

| Estratégia | Ideia | Custo para achar todas as pontes |
|---|---|---|
| **Naïve** | Remove cada aresta, testa se o grafo continua conexo e restaura a aresta | O(E · (V + E)) |
| **Tarjan** | Uma única DFS, usando tempos de descoberta e o valor *low* de cada vértice | O(V + E) |

Os experimentos medem o **tempo médio** das duas estratégias em grafos aleatórios **eulerianos**, **semi-eulerianos** e **não eulerianos**, com **100, 1.000, 10.000 e 100.000 vértices**.

---

## 📁 Estrutura do repositório

```
bridges-eulerian-path/
├── README.md
├── Grafo.java            ✅ estrutura do grafo + conectividade (BFS)
├── DetectorPontes.java   ✅ interface comum das duas estratégias
├── PontesNaive.java      ✅ método naïve
├── PontesTarjan.java     ✅ método de Tarjan
├── Fleury.java           ✅ caminho euleriano (recebe um DetectorPontes)
├── GeradorGrafos.java    ✅ grafos aleatórios (euleriano / semi / não euleriano)
├── Experimentos.java     ✅ execução, médias de tempo e saída em CSV
├── resultados/           CSVs gerados pelos experimentos
└── relatorio/            relatório em TeX (main.tex + seções + dados/ para os gráficos)
```

✅ pronto · ⏳ a fazer

---

## 👥 Divisão do grupo

| Integrante | Parte | Arquivos | Seção do relatório |
|---|---|---|---|
| **Luisa Campanha** | Estrutura do grafo, conectividade e método naïve | `Grafo`, `DetectorPontes`, `PontesNaive` | Introdução e definições |
| **Thayná Cota** | Tarjan, verificação euleriana e Fleury | `PontesTarjan`, `Fleury` | Implementação e complexidade |
| **Luan Oliveira** | Geradores de grafos e experimentos | `GeradorGrafos`, `Experimentos` | Experimentos e resultados |
| **Todos** | Conclusão e revisão | — | Conclusão |

> ⚠️ **Cada integrante precisa entregar o trabalho.** Quem não entregar leva zero, mesmo que os outros entreguem. Os fontes em TeX também são obrigatórios.

---

## 📄 Formato do arquivo de entrada

O mesmo formato dos trabalhos anteriores: a primeira linha tem `n` (vértices) e `m` (arestas), e cada linha seguinte tem uma aresta `u v`. Os vértices vão de **1 a n**.

```
7 8
1 2
2 3
3 1
3 4
4 5
5 6
6 4
6 7
```

Esse grafo é formado por dois triângulos (1-2-3 e 4-5-6) ligados pela aresta **3–4**, mais a aresta **6–7**. As pontes são **3–4** e **6–7**.

---

## ▶️ Como compilar e executar

Requisito: **Java 17 ou mais recente**.

```bash
javac -d build *.java
java -cp build PontesNaive      # pontes pelo naïve
java -cp build PontesTarjan     # pontes pelo Tarjan
java -cp build Fleury           # caminho euleriano (escolhe a estratégia)
java -cp build GeradorGrafos    # autoteste do gerador
```

### Experimentos

```bash
java -cp build Experimentos                 # tudo: 100, 1.000, 10.000 e 100.000 vértices, timeout de 30 min
java -cp build Experimentos 100,1000 60     # só alguns tamanhos, timeout de 60 s (teste rápido)
java -cp build Experimentos resumir         # refaz os resumos a partir de resultados/bruto.csv
```

Cada execução roda em um processo separado, e o timeout é aplicado matando esse processo. Os resultados ficam em:

- `resultados/bruto.csv`: uma linha por execução (`medida,n,m,tipo,estrategia,repeticao,semente,tempo_ms,status`);
- `resultados/<medida>_<tipo>.csv`: média e desvio padrão por `n`, com o naïve e o Tarjan lado a lado. Esses arquivos vão para `relatorio/dados/`, de onde os gráficos do relatório (pgfplots) leem os dados.

Medidas: `fleury` (caminho euleriano, nos 3 tipos) e `pontes` (`encontrarPontes` sozinho, em grafos não eulerianos).

O programa pede o nome do arquivo do grafo e mostra:

```
Nome do arquivo do grafo: ../grafos/pequeno.txt
Vertices: 7 | Arestas: 8
Conexo: sim

Pontes encontradas (2):
3 - 4
6 - 7
Tempo de execucao: 0.016 ms
```

---

## 🧠 Decisões de implementação

**Representação: vetor de inícios (CSR).** Os vizinhos de `v` ficam em `vizinho[inicio[v]]` até `vizinho[inicio[v+1] - 1]`. Os vetores são montados em O(V + E) com contagem de graus e soma acumulada, a mesma ideia do counting sort. Como o grafo é não-direcionado, cada aresta aparece nas duas listas.

**Remoção de aresta em O(1).** Cada aresta tem um `id` (de 0 a m-1), e remover é só marcar `removida[id] = true`. Quem percorre as listas pula as arestas removidas. Isso é essencial porque o Fleury remove arestas o tempo todo e o método naïve remove e restaura uma aresta a cada teste.

**BFS iterativa.** A busca usa uma fila própria (um vetor). Uma busca recursiva estouraria a pilha do Java (`StackOverflowError`) com 100.000 vértices. **O Tarjan também precisa ser iterativo.**

**Truque da marca.** Em vez de zerar o vetor de visitados antes de cada busca (O(V) toda vez), cada busca usa um número novo, e um vértice está visitado quando `marca[v] == marcaAtual`. Assim, uma busca que termina cedo custa só o que ela realmente percorreu.

**Teste de ponte no naïve.** A aresta {u, v} é ponte se, depois de removida, **u não alcança mais v**. Isso equivale a testar a conectividade do grafo inteiro, mas a busca pode parar assim que encontra `v`.

**Conectividade para Euler.** `estaConexo()` ignora vértices de grau 0, porque para caminho euleriano só importam os vértices que ainda têm arestas.

### Como percorrer os vizinhos de um vértice (para quem for usar o `Grafo`)

```java
for (int i = g.inicio[v]; i < g.inicio[v + 1]; i++) {
    int id = g.arestaDaPosicao[i];
    if (g.removida[id]) continue;   // pula arestas removidas
    int w = g.vizinho[i];
    // ... usar w ...
}
```

### Métodos principais do `Grafo`

| Método | O que faz |
|---|---|
| `Grafo.lerArquivo(nome)` | Lê um grafo de arquivo |
| `new Grafo(n, origens, destinos)` | Cria um grafo direto na memória (útil para o gerador) |
| `grau(v)` | Grau atual de `v`, descontando as arestas removidas |
| `removerAresta(id)` / `restaurarAresta(id)` | Remove ou restaura uma aresta em O(1) |
| `outroLado(id, v)` | Dado um lado da aresta, devolve o outro |
| `alcanca(u, v)` | Existe caminho entre `u` e `v`? |
| `estaConexo()` | O grafo é conexo (ignorando vértices isolados)? |

---

## ✅ Testes realizados (Luisa Campanha)

- **Grafo pequeno conferido à mão:** encontrou exatamente as pontes 3–4 e 6–7.
- **200 grafos aleatórios** (de 2 a 60 vértices, incluindo grafos desconexos), comparados com a função `bridges` da biblioteca networkx do Python: **0 diferenças**, tanto nas pontes quanto na conectividade.
- **Remoção e restauração:** depois de remover a aresta 1–2 do grafo de exemplo, as arestas 2–3 e 3–1 passam a ser pontes, como esperado.

### Tempo do naïve (todas as pontes, grafos esparsos com grau médio ~4)

| Vértices | Arestas | Tempo |
|---|---|---|
| 10.000 | 19.999 | ~2,5 s |
| 100.000 | 199.999 | ~6 min |

---

## 📊 Resultados dos experimentos

Rodada completa: 224 execuções, todas concluídas e corretas, sem nenhum timeout (limite de 30 min). Tempos médios com 100.000 vértices:

| Medida | Naïve | Tarjan |
|---|---|---|
| Fleury, grafo euleriano | ~3,0 min | ~22,5 min |
| Fleury, grafo semi-euleriano | ~2,7 min | ~18,1 min |
| Fleury, grafo não euleriano | 19 ms | 20 ms |
| Todas as pontes (`encontrarPontes`) | ~4,1 min | 42 ms |

Dentro do Fleury, o **naïve é mais rápido** que o Tarjan, de 2,8 a 7,5 vezes: o teste do naïve para assim que encontra a outra ponta da aresta, enquanto o Tarjan percorre a componente inteira a cada teste. Já para encontrar todas as pontes de uma vez, o Tarjan é linear e foi quase 6.000 vezes mais rápido. Os detalhes estão em `resultados/` e na seção de experimentos do relatório.

---

## 📚 Referência

Tarjan, R. E. (1974). *A note on finding the bridges of a graph.* Information Processing Letters, 2(6), 160–161. https://doi.org/10.1016/0020-0190(74)90003-9
