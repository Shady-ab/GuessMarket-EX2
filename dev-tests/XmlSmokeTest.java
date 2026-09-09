package guessmarket.engine;

public final class XmlSmokeTest {
    public static void main(String[] args) throws Exception {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        for (String path : args) {
            try {
                engine.loadFromXml(path);
                System.out.println("OK  " + path + " events=" + engine.getEvents().size()
                        + " users=" + engine.getUsers().size());
            } catch (GuessMarketException ex) {
                System.out.println("ERR " + path + " -> " + ex.getMessage());
            }
        }
    }
}
