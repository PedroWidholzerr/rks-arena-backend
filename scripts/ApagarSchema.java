import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Apaga uma schema TEMPORÁRIA de teste (nome começando com "teste_"). Recusa qualquer outra, em especial "public".
 * Uso (com DATABASE_URL, DB_USER e DB_PASSWORD no ambiente):
 *   java -cp postgresql-42.7.10.jar scripts/ApagarSchema.java teste_local
 */
public class ApagarSchema {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("teste_[a-z0-9_]+")) {
            System.err.println("Informe uma schema temporária, com nome 'teste_...' (letras minúsculas, números e _). Nada foi apagado.");
            System.exit(2);
        }
        String url = System.getenv("DATABASE_URL").replaceAll("&currentSchema=[^&]*", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement s = c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS " + args[0] + " CASCADE");
            System.out.println("Schema " + args[0] + " apagada.");
        }
    }
}
