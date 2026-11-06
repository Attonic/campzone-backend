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
 * próprio pacote {@code motor}, sem Spring, JPA, outros módulos nem fontes de não determinismo.
 */
class ArquiteturaMotorTest {

    private static final Path FONTES_DO_MOTOR = Path.of("src/main/java/br/com/codeacle/campzone/motor");
    private static final String PACOTE_DO_MOTOR = "br.com.codeacle.campzone.motor";

    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+?)(?:\\.\\*)?\\s*;",
            Pattern.MULTILINE);
    private static final Pattern LINHA_DE_PACOTE_OU_IMPORT = Pattern.compile("^\\s*(?:package|import)\\s[^;]*;",
            Pattern.MULTILINE);
    private static final Pattern COMENTARIO_OU_TEXTO = Pattern.compile(
            "/\\*.*?\\*/|//[^\\n]*|\"\"\".*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])+'", Pattern.DOTALL);
    /** Nome qualificado no corpo do código: raiz de pacote conhecida seguida de pelo menos mais um nível. */
    private static final Pattern NOME_QUALIFICADO = Pattern
            .compile("\\b(?:org|com|jakarta|javax|io|net|lombok|br)\\.[a-z]\\w*\\.\\w+");
    private static final Pattern NAO_DETERMINISMO = Pattern.compile(
            "\\bSecureRandom\\b|\\bMath\\.random\\b|\\bcurrentTimeMillis\\b|\\bnanoTime\\b|\\b(?:Instant|LocalDate|LocalDateTime"
                    + "|ZonedDateTime|OffsetDateTime|Clock)\\b|\\bThreadLocalRandom\\b|\\bUUID\\b");

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
            Matcher m = NAO_DETERMINISMO.matcher(semComentariosETextos(Files.readString(fonte)));
            while (m.find()) {
                violacoes.add(fonte.getFileName() + ": " + m.group());
            }
        }

        assertThat(violacoes).isEmpty();
    }

    // ---------------------------------------------------------------- o detector em si

    @Test
    void detectaImportsDeSpringJpaEOutrosModulos() {
        String fonte = """
                package br.com.codeacle.campzone.motor;

                import java.util.List;
                import static java.util.Objects.requireNonNull;
                import br.com.codeacle.campzone.motor.pontoscorridos.Classificacao;
                import org.springframework.stereotype.Service;
                import jakarta.persistence.Entity;
                import javax.sql.DataSource;
                import br.com.codeacle.campzone.campeonato.domain.CriterioDesempate;
                import static br.com.codeacle.campzone.participante.domain.Participante.*;
                import br.com.codeacle.campzone.motorista.Intruso;
                """;

        assertThat(importsProibidos(fonte)).containsExactly(
                "org.springframework.stereotype.Service",
                "jakarta.persistence.Entity",
                "javax.sql.DataSource",
                "br.com.codeacle.campzone.campeonato.domain.CriterioDesempate",
                "br.com.codeacle.campzone.participante.domain.Participante",
                "br.com.codeacle.campzone.motorista.Intruso");
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
                }
                """;

        assertThat(nomesQualificadosProibidos(fonte)).containsExactly("org.springframework.stereotype.Service");
    }

    // ---------------------------------------------------------------- varredura

    private static List<Path> fontes() throws IOException {
        assertThat(FONTES_DO_MOTOR).isDirectory();
        try (Stream<Path> arquivos = Files.walk(FONTES_DO_MOTOR)) {
            return arquivos.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static boolean permitido(String nome) {
        return nome.startsWith("java.") || nome.equals(PACOTE_DO_MOTOR) || nome.startsWith(PACOTE_DO_MOTOR + ".");
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

    private static String semComentariosETextos(String fonte) {
        return COMENTARIO_OU_TEXTO.matcher(fonte).replaceAll(" ");
    }
}
