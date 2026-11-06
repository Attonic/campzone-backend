package br.com.codeacle.campzone.motor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Garante o isolamento do motor (ADR 0005) varrendo as fontes, sem dependência de teste extra: só {@code java.*} e o
 * próprio pacote {@code motor}, sem Spring, JPA, outros módulos, I/O (arquivos, rede, banco) nem fontes de não
 * determinismo (relógio, {@code Random} sem seed, {@code shuffle} sem {@code Random}).
 */
class ArquiteturaMotorTest {

    private static final Path FONTES_DO_MOTOR = Path.of("src/main/java/br/com/codeacle/campzone/motor");
    private static final String PACOTE_DO_MOTOR = "br.com.codeacle.campzone.motor";
    /** Pacotes do JDK que o motor não pode tocar: o motor é puro e não faz I/O. */
    private static final List<String> PACOTES_JAVA_PROIBIDOS = List.of("java.io", "java.nio.file", "java.net", "java.sql");

    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+?)(?:\\.\\*)?\\s*;",
            Pattern.MULTILINE);
    private static final Pattern LINHA_DE_PACOTE_OU_IMPORT = Pattern.compile("^\\s*(?:package|import)\\s[^;]*;",
            Pattern.MULTILINE);
    private static final Pattern COMENTARIO_OU_TEXTO = Pattern.compile(
            "/\\*.*?\\*/|//[^\\n]*|\"\"\".*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])+'", Pattern.DOTALL);
    /** Nome qualificado no corpo do código: raiz de pacote conhecida seguida de pelo menos mais um nível. */
    private static final Pattern NOME_QUALIFICADO = Pattern
            .compile("\\b(?:java|tools|org|com|jakarta|javax|io|net|lombok|br)\\.[a-z]\\w*\\.\\w+");
    private static final Pattern NAO_DETERMINISMO = Pattern.compile(
            "\\bSecureRandom\\b|\\bMath\\.random\\b|\\bcurrentTimeMillis\\b|\\bnanoTime\\b|\\b(?:Instant|LocalDate|LocalDateTime"
                    + "|ZonedDateTime|OffsetDateTime|Clock)\\b|\\bThreadLocalRandom\\b|\\bUUID\\b");
    /** {@code new Random()} (ou {@code SplittableRandom}) sem argumento: a seed viria do relógio ou do sistema. */
    private static final Pattern RANDOM_SEM_SEED = Pattern
            .compile("\\bnew\\s+(?:java\\s*\\.\\s*util\\s*\\.\\s*)?(?:Splittable)?Random\\s*\\(\\s*\\)");
    private static final Pattern CHAMADA_SHUFFLE = Pattern.compile("\\bshuffle\\s*\\(");

    @Test
    void asFontesDoMotorForamEncontradas() throws IOException {
        // evita um "verde vazio" se a pasta mudar de lugar
        assertThat(fontes()).hasSizeGreaterThanOrEqualTo(15);
    }

    @Test
    void motorImportaSoJavaEOProprioMotor() throws IOException {
        List<String> violacoes = new ArrayList<>();
        for (Path fonte : fontes()) {
            violacoes.addAll(importsProibidos(Files.readString(fonte)).stream().map(i -> fonte.getFileName() + ": " + i)
                    .toList());
        }

        assertThat(violacoes).isEmpty();
    }

    @Test
    void motorNaoUsaNomesQualificadosDeOutrosPacotesNoCorpo() throws IOException {
        List<String> violacoes = new ArrayList<>();
        for (Path fonte : fontes()) {
            violacoes.addAll(nomesQualificadosProibidos(Files.readString(fonte)).stream()
                    .map(n -> fonte.getFileName() + ": " + n).toList());
        }

        assertThat(violacoes).isEmpty();
    }

    @Test
    void motorNaoUsaFontesDeNaoDeterminismo() throws IOException {
        List<String> violacoes = new ArrayList<>();
        for (Path fonte : fontes()) {
            violacoes.addAll(naoDeterminismo(Files.readString(fonte)).stream().map(n -> fonte.getFileName() + ": " + n)
                    .toList());
        }

        assertThat(violacoes).isEmpty();
    }

    // ---------------------------------------------------------------- o detector em si

    @Test
    void detectaImportsDeSpringJpaEOutrosModulos() {
        String fonte = """
                package br.com.codeacle.campzone.motor;

                import java.util.List;
                import java.nio.charset.StandardCharsets;
                import static java.util.Objects.requireNonNull;
                import br.com.codeacle.campzone.motor.pontoscorridos.Classificacao;
                import org.springframework.stereotype.Service;
                import jakarta.persistence.Entity;
                import javax.sql.DataSource;
                import br.com.codeacle.campzone.campeonato.domain.CriterioDesempate;
                import static br.com.codeacle.campzone.participante.domain.Participante.*;
                import br.com.codeacle.campzone.motorista.Intruso;
                import tools.jackson.databind.ObjectMapper;
                """;

        assertThat(importsProibidos(fonte)).containsExactly(
                "org.springframework.stereotype.Service",
                "jakarta.persistence.Entity",
                "javax.sql.DataSource",
                "br.com.codeacle.campzone.campeonato.domain.CriterioDesempate",
                "br.com.codeacle.campzone.participante.domain.Participante",
                "br.com.codeacle.campzone.motorista.Intruso",
                "tools.jackson.databind.ObjectMapper");
    }

    @Test
    void detectaImportsDeIoArquivosRedeEBancoDoJdk() {
        String fonte = """
                package br.com.codeacle.campzone.motor;

                import java.io.File;
                import java.io.*;
                import java.nio.file.Files;
                import java.nio.file.*;
                import java.net.URI;
                import java.net.http.HttpClient;
                import java.sql.Connection;
                import java.sql.*;
                import static java.nio.file.Files.readString;
                import java.nio.ByteBuffer;
                import java.nio.charset.StandardCharsets;
                import java.util.concurrent.atomic.AtomicInteger;
                """;

        assertThat(importsProibidos(fonte)).containsExactly(
                "java.io.File",
                "java.io",
                "java.nio.file.Files",
                "java.nio.file",
                "java.net.URI",
                "java.net.http.HttpClient",
                "java.sql.Connection",
                "java.sql",
                "java.nio.file.Files.readString");
    }

    @Test
    void detectaNomeQualificadoNoCorpoMasIgnoraComentariosETextos() {
        String fonte = """
                package br.com.codeacle.campzone.motor;

                /** Veja {@link br.com.codeacle.campzone.campeonato.domain.Campeonato}. */
                class X {
                    // org.springframework.stereotype.Service no comentário
                    String texto = "org.springframework.stereotype.Service";
                    java.util.List<String> ok;
                    br.com.codeacle.campzone.motor.Y tambemOk;
                    org.springframework.stereotype.Service proibido;
                    tools.jackson.databind.ObjectMapper tambemProibido;
                    java.io.File arquivo;
                    java.nio.file.Path caminho;
                    java.net.URI uri;
                    java.sql.Connection conexao;
                    java.nio.charset.Charset charset;
                }
                """;

        assertThat(nomesQualificadosProibidos(fonte)).containsExactly(
                "org.springframework.stereotype.Service",
                "tools.jackson.databind.ObjectMapper",
                "java.io.File",
                "java.nio.file.Path",
                "java.net.URI",
                "java.sql.Connection");
    }

    @Test
    void detectaRandomSemSeedEShuffleSemRandom() {
        String fonte = """
                class X {
                    Random semSeed = new Random();
                    Random comEspacos = new  Random ( );
                    Random qualificado = new java.util.Random();
                    SplittableRandom dividido = new SplittableRandom();
                    Random comSeed = new Random(42L);
                    Random comSeedCalculada = new Random(seedSorteio + 1);

                    void f() {
                        Collections.shuffle(lista);
                        shuffle(lista);
                        Collections.shuffle(new ArrayList<Map<String, Long>>(origem));
                        Collections.shuffle(copiar(a, b));
                        Collections.shuffle(lista, new Random(seedSorteio));
                        Collections.shuffle(lista, rnd);
                        Collections.shuffle(new ArrayList<Map<String, Long>>(origem), rnd);
                    }
                }
                """;

        assertThat(naoDeterminismo(fonte)).containsExactly(
                "new Random()",
                "new  Random ( )",
                "new java.util.Random()",
                "new SplittableRandom()",
                "shuffle(lista)",
                "shuffle(lista)",
                "shuffle(new ArrayList<Map<String, Long>>(origem))",
                "shuffle(copiar(a, b))");
    }

    @Test
    void ignoraRandomEShuffleEmComentariosETextos() {
        String fonte = """
                class X {
                    // new Random() no comentário e Collections.shuffle(lista)
                    /** {@code new Random()} e {@code shuffle(lista)} */
                    String texto = "new Random() shuffle(lista)";
                }
                """;

        assertThat(naoDeterminismo(fonte)).isEmpty();
    }

    @Test
    void detectaRelogioUuidESecureRandom() {
        String fonte = """
                class X {
                    long a = System.currentTimeMillis();
                    long b = System.nanoTime();
                    double c = Math.random();
                    Object d = java.util.UUID.randomUUID();
                    Object e = new SecureRandom();
                    Object f = Instant.now();
                }
                """;

        assertThat(naoDeterminismo(fonte)).containsExactly("currentTimeMillis", "nanoTime", "Math.random", "UUID",
                "SecureRandom", "Instant");
    }

    // ---------------------------------------------------------------- varredura

    private static List<Path> fontes() throws IOException {
        assertThat(FONTES_DO_MOTOR).isDirectory();
        try (Stream<Path> arquivos = Files.walk(FONTES_DO_MOTOR)) {
            return arquivos.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static boolean permitido(String nome) {
        if (nome.equals(PACOTE_DO_MOTOR) || nome.startsWith(PACOTE_DO_MOTOR + ".")) {
            return true;
        }
        return nome.startsWith("java.") && PACOTES_JAVA_PROIBIDOS.stream()
                .noneMatch(proibido -> nome.equals(proibido) || nome.startsWith(proibido + "."));
    }

    private static List<String> importsProibidos(String fonte) {
        List<String> proibidos = new ArrayList<>();
        Matcher m = IMPORT.matcher(fonte);
        while (m.find()) {
            String importado = m.group(1);
            if (!permitido(importado)) {
                proibidos.add(importado);
            }
        }
        return proibidos;
    }

    private static List<String> nomesQualificadosProibidos(String fonte) {
        String corpo = LINHA_DE_PACOTE_OU_IMPORT.matcher(semComentariosETextos(fonte)).replaceAll("");
        List<String> proibidos = new ArrayList<>();
        Matcher m = NOME_QUALIFICADO.matcher(corpo);
        while (m.find()) {
            // alcança o nome completo (com todos os níveis) para testar o prefixo permitido
            int fim = m.end();
            while (fim < corpo.length() && (Character.isJavaIdentifierPart(corpo.charAt(fim)) || corpo.charAt(fim) == '.')) {
                fim++;
            }
            String nome = corpo.substring(m.start(), fim);
            if (!permitido(nome)) {
                proibidos.add(nome);
            }
        }
        return proibidos;
    }

    /** Usos de relógio, UUID, geradores seguros e sorteios sem seed, na ordem em que aparecem no código. */
    private static List<String> naoDeterminismo(String fonte) {
        String corpo = semComentariosETextos(fonte);
        List<int[]> achados = new ArrayList<>();
        for (Pattern padrao : List.of(NAO_DETERMINISMO, RANDOM_SEM_SEED)) {
            Matcher m = padrao.matcher(corpo);
            while (m.find()) {
                achados.add(new int[] { m.start(), m.end() });
            }
        }
        achados.addAll(shufflesSemRandom(corpo));
        return achados.stream().sorted((a, b) -> Integer.compare(a[0], b[0]))
                .map(faixa -> corpo.substring(faixa[0], faixa[1])).toList();
    }

    /**
     * Chamadas {@code shuffle(x)} com um único argumento, que sorteiam com um {@code Random} interno sem seed. Conta as
     * vírgulas fora de parênteses, colchetes, chaves e genéricos; é uma heurística de varredura, não um parser.
     */
    private static List<int[]> shufflesSemRandom(String corpo) {
        List<int[]> faixas = new ArrayList<>();
        Matcher m = CHAMADA_SHUFFLE.matcher(corpo);
        while (m.find()) {
            int profundidade = 0;
            boolean doisArgumentos = false;
            int i = m.end();
            for (; i < corpo.length(); i++) {
                char c = corpo.charAt(i);
                if (c == ')' && profundidade == 0) {
                    break;
                }
                if (c == '(' || c == '[' || c == '{' || c == '<') {
                    profundidade++;
                } else if (c == ')' || c == ']' || c == '}' || (c == '>' && corpo.charAt(i - 1) != '-')) {
                    profundidade = Math.max(0, profundidade - 1);
                } else if (c == ',' && profundidade == 0) {
                    doisArgumentos = true;
                }
            }
            if (!doisArgumentos) {
                faixas.add(new int[] { m.start(), Math.min(i + 1, corpo.length()) });
            }
        }
        return faixas;
    }

    private static String semComentariosETextos(String fonte) {
        return COMENTARIO_OU_TEXTO.matcher(fonte).replaceAll(" ");
    }
}
