package org.exampleorg.example.pow4.Pow4.blockdata;

import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.sql.PreparedStatement;



import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Blockdata extends JavaPlugin {

    private static final String FOLDER_PATH = "plugins/blockdata";
    private static final String CONFIG_FILE = FOLDER_PATH + "/config.json";
    private static final String MESSAGES_FILE = FOLDER_PATH + "/messages.json";

    private final Map<String, String> lockedChests = Collections.synchronizedMap(new HashMap<>());


    private final LockedChests lockedChestsManager = new LockedChests();
    private Connection connection;

    @Override
    public void onEnable() {
        getLogger().info("LockChestPlugin habilitado!");

        createFolderAndConfig();
        boolean isEnabled = loadPluginStatus();

        if (!isEnabled) {
            getLogger().warning("Plugin desativado via configuração.");
            getServer().getPluginManager().disablePlugin(this);
            return; // Finaliza a inicialização se o plugin estiver desativado
        }

        // Configuração do banco de dados
        setupDatabaseConnection(); // Configura a conexão
        setupDatabase(); // Cria a tabela se necessário

        // Carrega os baús trancados do banco de dados
        loadLockedChests(); // Certifique-se de que este método é chamado após as etapas acima



        // Registra eventos e comandos
        ChestLockListener chestLockListener = new ChestLockListener(this);
        getServer().getPluginManager().registerEvents(chestLockListener, this);
        this.getCommand("lock").setExecutor(new LockChestCommand(chestLockListener));
        this.getCommand("unlock").setExecutor(new LockChestCommand(chestLockListener));
        this.getCommand("viewpassword").setExecutor(new LockChestCommand(chestLockListener));


        // Carregar idioma
        String language = loadLanguage();
        getLogger().info("Idioma configurado: " + language);
    }

    @Override
    public void onDisable() {
        getLogger().info("LockChestPlugin desabilitado!");
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Conexão com o banco de dados fechada.");
            } catch (Exception e) {
                getLogger().severe("Erro ao fechar a conexão: " + e.getMessage());
            }
        }
    }

    private void createFolderAndConfig() {
        File folder = new File(FOLDER_PATH);
        if (!folder.exists() && folder.mkdirs()) {
            getLogger().info("Pasta de configuração criada em: " + FOLDER_PATH);
        }

        // Criação de config.json
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            try (FileWriter writer = new FileWriter(configFile)) {
                JsonObject defaultConfig = new JsonObject();
                defaultConfig.addProperty("enabled", true);
                defaultConfig.addProperty("language", "br");

                // Gson com Pretty Printing
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(defaultConfig, writer);

                getLogger().info("Arquivo config.json criado com configurações padrão.");
            } catch (IOException e) {
                getLogger().severe("Erro ao criar config.json: " + e.getMessage());
            }
        }

        // Criação de messages.json

        File messagesFile = new File(MESSAGES_FILE);
        if (!messagesFile.exists()) {
            try (FileWriter writer = new FileWriter(messagesFile)) {
                JsonObject messages = new JsonObject();

                JsonObject lockMessages = new JsonObject();
                lockMessages.addProperty("br", "Baú trancado com a senha: {password}."); // Português (BR)
                lockMessages.addProperty("en", "Chest locked with password: {password}."); // Inglês (EN)
                lockMessages.addProperty("es", "Cofre bloqueado con la contraseña: {password}."); // Espanhol (ES)
                lockMessages.addProperty("fr", "Coffre verrouillé avec le mot de passe : {password}."); // Francês (FR)
                lockMessages.addProperty("de", "Truhe mit Passwort gesperrt: {password}."); // Alemão (DE)
                lockMessages.addProperty("ru", "Сундук заперт с паролем: {password}."); // Russo (RU)
                lockMessages.addProperty("zh", "使用密码 {password} 锁定箱子。"); // Chinês Simplificado (ZH)
                lockMessages.addProperty("zh-tw", "使用密碼 {password} 鎖定箱子。"); // Chinês Tradicional (ZH-TW)
                lockMessages.addProperty("ja", "パスワード {password} でチェストがロックされています。"); // Japonês (JA)
                lockMessages.addProperty("ko", "비밀번호 {password}로 상자가 잠겼습니다."); // Coreano (KO)
                lockMessages.addProperty("it", "Baule bloccato con la password: {password}."); // Italiano (IT)
                lockMessages.addProperty("nl", "Kist vergrendeld met wachtwoord: {password}."); // Holandês (NL)
                lockMessages.addProperty("pl", "Skrzynia zablokowana hasłem: {password}."); // Polonês (PL)
                lockMessages.addProperty("sv", "Bröstet är låst med lösenordet: {password}."); // Sueco (SV)
                lockMessages.addProperty("cs", "Truhla zamčená heslem: {password}."); // Tcheco (CS)
                lockMessages.addProperty("hu", "A láda jelszóval van lezárva: {password}."); // Húngaro (HU)
                lockMessages.addProperty("tr", "Sandık şifreyle kilitlendi: {password}."); // Turco (TR)
                lockMessages.addProperty("ar", "تم قفل الصندوق بكلمة المرور: {password}."); // Árabe (AR)
                lockMessages.addProperty("fi", "Arkku lukittu salasanalla: {password}."); // Finlandês (FI)
                lockMessages.addProperty("da", "Kisten er låst med adgangskoden: {password}."); // Dinamarquês (DA)


                JsonObject unlockMessages = new JsonObject();
                unlockMessages.addProperty("br", "Baú destrancado com sucesso!"); // Português (BR)
                unlockMessages.addProperty("en", "Chest successfully unlocked!"); // Inglês (EN)
                unlockMessages.addProperty("es", "Cofre desbloqueado con éxito!"); // Espanhol (ES)
                unlockMessages.addProperty("fr", "Coffre déverrouillé avec succès !"); // Francês (FR)
                unlockMessages.addProperty("de", "Truhe erfolgreich entsperrt!"); // Alemão (DE)
                unlockMessages.addProperty("ru", "Сундук успешно открыт!"); // Russo (RU)
                unlockMessages.addProperty("zh", "箱子已成功解锁！"); // Chinês Simplificado (ZH)
                unlockMessages.addProperty("zh-tw", "箱子已成功解鎖！"); // Chinês Tradicional (ZH-TW)
                unlockMessages.addProperty("ja", "チェストのロックを解除しました！"); // Japonês (JA)
                unlockMessages.addProperty("ko", "상자가 성공적으로 잠금 해제되었습니다!"); // Coreano (KO)
                unlockMessages.addProperty("it", "Baule sbloccato con successo!"); // Italiano (IT)
                unlockMessages.addProperty("nl", "Kist succesvol ontgrendeld!"); // Holandês (NL)
                unlockMessages.addProperty("pl", "Skrzynia pomyślnie odblokowana!"); // Polonês (PL)
                unlockMessages.addProperty("sv", "Bröstet är framgångsrikt upplåst!"); // Sueco (SV)
                unlockMessages.addProperty("cs", "Truhla byla úspěšně odemčena!"); // Tcheco (CS)
                unlockMessages.addProperty("hu", "A láda sikeresen feloldva!"); // Húngaro (HU)
                unlockMessages.addProperty("tr", "Sandık başarıyla kilidi açıldı!"); // Turco (TR)
                unlockMessages.addProperty("ar", "تم فتح الصندوق بنجاح!"); // Árabe (AR)
                unlockMessages.addProperty("fi", "Arkku avattiin onnistuneesti!"); // Finlandês (FI)
                unlockMessages.addProperty("da", "Kisten blev låst op med succes!"); // Dinamarquês (DA)


                JsonObject incorrectPasswordMessage = new JsonObject();
                incorrectPasswordMessage.addProperty("br", "Senha incorreta. Use a etiqueta correta!"); // Português (BR)
                incorrectPasswordMessage.addProperty("en", "Incorrect password. Use the correct name tag!"); // Inglês (EN)
                incorrectPasswordMessage.addProperty("es", "Contraseña incorrecta. Use la etiqueta correcta!"); // Espanhol (ES)
                incorrectPasswordMessage.addProperty("fr", "Mot de passe incorrect. Utilisez la bonne étiquette!"); // Francês (FR)
                incorrectPasswordMessage.addProperty("de", "Falsches Passwort. Verwenden Sie das richtige Namensschild!"); // Alemão (DE)
                incorrectPasswordMessage.addProperty("ru", "Неверный пароль. Используйте правильный идентификатор!"); // Russo (RU)
                incorrectPasswordMessage.addProperty("zh", "密码错误。请使用正确的名称标签！"); // Chinês Simplificado (ZH)
                incorrectPasswordMessage.addProperty("zh-tw", "密碼錯誤。請使用正確的名稱標籤！"); // Chinês Tradicional (ZH-TW)
                incorrectPasswordMessage.addProperty("ja", "パスワードが間違っています。正しい名前タグを使用してください！"); // Japonês (JA)
                incorrectPasswordMessage.addProperty("ko", "비밀번호가 올바르지 않습니다. 올바른 이름 태그를 사용하세요!"); // Coreano (KO)
                incorrectPasswordMessage.addProperty("it", "Password errata. Usa il tag nome corretto!"); // Italiano (IT)
                incorrectPasswordMessage.addProperty("nl", "Onjuist wachtwoord. Gebruik de juiste naamtag!"); // Holandês (NL)
                incorrectPasswordMessage.addProperty("pl", "Nieprawidłowe hasło. Użyj poprawnej etykiety nazwy!"); // Polonês (PL)
                incorrectPasswordMessage.addProperty("sv", "Fel lösenord. Använd rätt namnlapp!"); // Sueco (SV)
                incorrectPasswordMessage.addProperty("cs", "Nesprávné heslo. Použijte správný jmenovku!"); // Tcheco (CS)
                incorrectPasswordMessage.addProperty("hu", "Helytelen jelszó. Használja a megfelelő névcímkét!"); // Húngaro (HU)
                incorrectPasswordMessage.addProperty("tr", "Hatalı şifre. Doğru ad etiketini kullanın!"); // Turco (TR)
                incorrectPasswordMessage.addProperty("ar", "كلمة المرور غير صحيحة. استخدم علامة الاسم الصحيحة!"); // Árabe (AR)
                incorrectPasswordMessage.addProperty("fi", "Väärä salasana. Käytä oikeaa nimeä tunnistetta!"); // Finlandês (FI)
                incorrectPasswordMessage.addProperty("da", "Forkert kodeord. Brug det korrekte navn tag!"); // Dinamarquês (DA)


                JsonObject lockedChestMessage = new JsonObject();
                lockedChestMessage.addProperty("br", "O baú está trancado. Segure a etiqueta correta ou use /unlock."); // Português (BR)
                lockedChestMessage.addProperty("en", "The chest is locked. Hold the correct name tag or use /unlock."); // Inglês (EN)
                lockedChestMessage.addProperty("es", "El cofre está bloqueado. Sostén la etiqueta correcta o usa /unlock."); // Espanhol (ES)
                lockedChestMessage.addProperty("fr", "Le coffre est verrouillé. Tenez la bonne étiquette ou utilisez /unlock."); // Francês (FR)
                lockedChestMessage.addProperty("de", "Die Truhe ist gesperrt. Halten Sie das richtige Namensschild oder verwenden Sie /unlock."); // Alemão (DE)
                lockedChestMessage.addProperty("ru", "Сундук заблокирован. Держите правильный идентификатор или используйте /unlock."); // Russo (RU)
                lockedChestMessage.addProperty("zh", "箱子被锁住了。请拿正确的名称标签或使用 /unlock。"); // Chinês Simplificado (ZH)
                lockedChestMessage.addProperty("zh-tw", "箱子被鎖住了。請拿正確的名稱標籤或使用 /unlock。"); // Chinês Tradicional (ZH-TW)
                lockedChestMessage.addProperty("ja", "チェストはロックされています。正しい名前タグを持つか /unlock を使用してください。"); // Japonês (JA)
                lockedChestMessage.addProperty("ko", "상자가 잠겨 있습니다. 올바른 이름 태그를 들거나 /unlock를 사용하세요."); // Coreano (KO)
                lockedChestMessage.addProperty("it", "Il baule è bloccato. Tieni il tag corretto o usa /unlock."); // Italiano (IT)
                lockedChestMessage.addProperty("nl", "De kist is vergrendeld. Houd de juiste naamtag vast of gebruik /unlock."); // Holandês (NL)
                lockedChestMessage.addProperty("pl", "Skrzynia jest zablokowana. Trzymaj poprawną etykietę lub użyj /unlock."); // Polonês (PL)
                lockedChestMessage.addProperty("sv", "Bröstet är låst. Håll rätt namnlapp eller använd /unlock."); // Sueco (SV)
                lockedChestMessage.addProperty("cs", "Truhla je zamčená. Držte správný štítek nebo použijte /unlock."); // Tcheco (CS)
                lockedChestMessage.addProperty("hu", "A láda le van zárva. Tartsa a megfelelő címkét, vagy használja a /unlock parancsot."); // Húngaro (HU)
                lockedChestMessage.addProperty("tr", "Sandık kilitli. Doğru isim etiketini tutun veya /unlock kullanın."); // Turco (TR)
                lockedChestMessage.addProperty("ar", "الصندوق مغلق. احمل بطاقة الاسم الصحيحة أو استخدم /unlock."); // Árabe (AR)
                lockedChestMessage.addProperty("fi", "Arkku on lukittu. Pidä oikeaa nimitarraa tai käytä /unlock."); // Finlandês (FI)
                lockedChestMessage.addProperty("da", "Kisten er låst. Hold det korrekte navneskilt eller brug /unlock."); // Dinamarquês (DA)


                JsonObject relockChestMessage = new JsonObject();
                relockChestMessage.addProperty("br", "O baú foi trancado novamente com a senha original."); // Português (BR)
                relockChestMessage.addProperty("en", "The chest has been relocked with the original password."); // Inglês (EN)
                relockChestMessage.addProperty("es", "El cofre se ha bloqueado nuevamente con la contraseña original."); // Espanhol (ES)
                relockChestMessage.addProperty("fr", "Le coffre a été reverrouillé avec le mot de passe original."); // Francês (FR)
                relockChestMessage.addProperty("de", "Die Truhe wurde mit dem ursprünglichen Passwort erneut gesperrt."); // Alemão (DE)
                relockChestMessage.addProperty("ru", "Сундук был снова заперт с исходным паролем."); // Russo (RU)
                relockChestMessage.addProperty("zh", "箱子已使用原始密码重新锁定。"); // Chinês Simplificado (ZH)
                relockChestMessage.addProperty("zh-tw", "箱子已使用原始密碼重新鎖定。"); // Chinês Tradicional (ZH-TW)
                relockChestMessage.addProperty("ja", "チェストは元のパスワードで再ロックされました。"); // Japonês (JA)
                relockChestMessage.addProperty("ko", "상자가 원래 비밀번호로 다시 잠겼습니다."); // Coreano (KO)
                relockChestMessage.addProperty("it", "Il baule è stato bloccato di nuovo con la password originale."); // Italiano (IT)
                relockChestMessage.addProperty("nl", "De kist is opnieuw vergrendeld met het originele wachtwoord."); // Holandês (NL)
                relockChestMessage.addProperty("pl", "Skrzynia została ponownie zablokowana oryginalnym hasłem."); // Polonês (PL)
                relockChestMessage.addProperty("sv", "Bröstet har låsts igen med det ursprungliga lösenordet."); // Sueco (SV)
                relockChestMessage.addProperty("cs", "Truhla byla znovu zamčena původním heslem."); // Tcheco (CS)
                relockChestMessage.addProperty("hu", "A láda újra lezárva az eredeti jelszóval."); // Húngaro (HU)
                relockChestMessage.addProperty("tr", "Sandık orijinal şifre ile yeniden kilitlendi."); // Turco (TR)
                relockChestMessage.addProperty("ar", "تم إعادة قفل الصندوق بكلمة المرور الأصلية."); // Árabe (AR)
                relockChestMessage.addProperty("fi", "Arkku on lukittu uudelleen alkuperäisellä salasanalla."); // Finlandês (FI)
                relockChestMessage.addProperty("da", "Kisten blev låst igen med den oprindelige adgangskode."); // Dinamarquês (DA)


                JsonObject unlockedTempMessage = new JsonObject();
                unlockedTempMessage.addProperty("br", "Baú destrancado com sucesso! Será trancado novamente em 5 segundos."); // Português (BR)
                unlockedTempMessage.addProperty("en", "Chest successfully unlocked! It will be relocked in 5 seconds."); // Inglês (EN)
                unlockedTempMessage.addProperty("es", "Cofre desbloqueado con éxito! Se bloqueará de nuevo en 5 segundos."); // Espanhol (ES)
                unlockedTempMessage.addProperty("fr", "Coffre déverrouillé avec succès! Il sera reverrouillé dans 5 secondes."); // Francês (FR)
                unlockedTempMessage.addProperty("de", "Truhe erfolgreich entsperrt! Sie wird in 5 Sekunden wieder gesperrt."); // Alemão (DE)
                unlockedTempMessage.addProperty("ru", "Сундук успешно открыт! Он будет снова заперт через 5 секунд."); // Russo (RU)
                unlockedTempMessage.addProperty("zh", "箱子已成功解锁！将在5秒内重新锁定。"); // Chinês Simplificado (ZH)
                unlockedTempMessage.addProperty("zh-tw", "箱子已成功解鎖！將在 5 秒內重新鎖定。"); // Chinês Tradicional (ZH-TW)
                unlockedTempMessage.addProperty("ja", "チェストが正常にアンロックされました！5秒後に再ロックされます。"); // Japonês (JA)
                unlockedTempMessage.addProperty("ko", "상자가 성공적으로 잠금 해제되었습니다! 5초 후에 다시 잠깁니다."); // Coreano (KO)
                unlockedTempMessage.addProperty("it", "Baule sbloccato con successo! Verrà bloccato di nuovo tra 5 secondi."); // Italiano (IT)
                unlockedTempMessage.addProperty("nl", "Kist succesvol ontgrendeld! Het wordt over 5 seconden opnieuw vergrendeld."); // Holandês (NL)
                unlockedTempMessage.addProperty("pl", "Skrzynia pomyślnie odblokowana! Zostanie ponownie zablokowana za 5 sekund."); // Polonês (PL)
                unlockedTempMessage.addProperty("sv", "Bröstet är framgångsrikt upplåst! Det kommer att låsas igen om 5 sekunder."); // Sueco (SV)
                unlockedTempMessage.addProperty("cs", "Truhla byla úspěšně odemčena! Bude znovu uzamčena za 5 sekund."); // Tcheco (CS)
                unlockedTempMessage.addProperty("hu", "A láda sikeresen feloldva! 5 másodperc múlva újra le lesz zárva."); // Húngaro (HU)
                unlockedTempMessage.addProperty("tr", "Sandık başarıyla kilidi açıldı! 5 saniye içinde tekrar kilitlenecek."); // Turco (TR)
                unlockedTempMessage.addProperty("ar", "تم فتح الصندوق بنجاح! سيتم إعادة قفله خلال 5 ثوانٍ."); // Árabe (AR)
                unlockedTempMessage.addProperty("fi", "Arkku avattiin onnistuneesti! Se lukitaan uudelleen 5 sekunnissa."); // Finlandês (FI)
                unlockedTempMessage.addProperty("da", "Kisten blev låst op med succes! Den låses igen om 5 sekunder."); // Dinamarquês (DA)


                JsonObject blockBreakMessage = new JsonObject();
                blockBreakMessage.addProperty("br", "Você não pode destruir um baú trancado."); // Português (BR)
                blockBreakMessage.addProperty("en", "You cannot destroy a locked chest."); // Inglês (EN)
                blockBreakMessage.addProperty("es", "No puedes destruir un cofre bloqueado."); // Espanhol (ES)
                blockBreakMessage.addProperty("fr", "Vous ne pouvez pas détruire un coffre verrouillé."); // Francês (FR)
                blockBreakMessage.addProperty("de", "Sie können keine gesperrte Truhe zerstören."); // Alemão (DE)
                blockBreakMessage.addProperty("ru", "Вы не можете разрушить запертый сундук."); // Russo (RU)
                blockBreakMessage.addProperty("zh", "您无法摧毁锁定的箱子。"); // Chinês Simplificado (ZH)
                blockBreakMessage.addProperty("zh-tw", "您無法摧毀已鎖定的箱子。"); // Chinês Tradicional (ZH-TW)
                blockBreakMessage.addProperty("ja", "ロックされたチェストは破壊できません。"); // Japonês (JA)
                blockBreakMessage.addProperty("ko", "잠긴 상자를 파괴할 수 없습니다."); // Coreano (KO)
                blockBreakMessage.addProperty("it", "Non puoi distruggere un baule bloccato."); // Italiano (IT)
                blockBreakMessage.addProperty("nl", "Je kunt geen vergrendelde kist vernietigen."); // Holandês (NL)
                blockBreakMessage.addProperty("pl", "Nie możesz zniszczyć zablokowanej skrzyni."); // Polonês (PL)
                blockBreakMessage.addProperty("sv", "Du kan inte förstöra ett låst bröst."); // Sueco (SV)
                blockBreakMessage.addProperty("cs", "Nemůžete zničit uzamčenou truhlu."); // Tcheco (CS)
                blockBreakMessage.addProperty("hu", "Nem pusztíthatod el a lezárt ládát."); // Húngaro (HU)
                blockBreakMessage.addProperty("tr", "Kilitli sandığı yok edemezsiniz."); // Turco (TR)
                blockBreakMessage.addProperty("ar", "لا يمكنك تدمير الصندوق المقفل."); // Árabe (AR)
                blockBreakMessage.addProperty("fi", "Et voi tuhota lukittua arkkua."); // Finlandês (FI)
                blockBreakMessage.addProperty("da", "Du kan ikke ødelægge en låst kiste."); // Dinamarquês (DA)


                JsonObject lockSuccessMessage = new JsonObject();
                lockSuccessMessage.addProperty("br", "Baú trancado com sucesso!"); // Português (BR)
                lockSuccessMessage.addProperty("en", "Chest successfully locked!"); // Inglês (EN)
                lockSuccessMessage.addProperty("es", "Cofre bloqueado con éxito!"); // Espanhol (ES)
                lockSuccessMessage.addProperty("fr", "Coffre verrouillé avec succès!"); // Francês (FR)
                lockSuccessMessage.addProperty("de", "Truhe erfolgreich gesperrt!"); // Alemão (DE)
                lockSuccessMessage.addProperty("ru", "Сундук успешно заперт!"); // Russo (RU)
                lockSuccessMessage.addProperty("zh", "箱子已成功锁定！"); // Chinês Simplificado (ZH)
                lockSuccessMessage.addProperty("zh-tw", "箱子已成功鎖定！"); // Chinês Tradicional (ZH-TW)
                lockSuccessMessage.addProperty("ja", "チェストが正常にロックされました！"); // Japonês (JA)
                lockSuccessMessage.addProperty("ko", "상자가 성공적으로 잠겼습니다!"); // Coreano (KO)
                lockSuccessMessage.addProperty("it", "Baule bloccato con successo!"); // Italiano (IT)
                lockSuccessMessage.addProperty("nl", "Kist succesvol vergrendeld!"); // Holandês (NL)
                lockSuccessMessage.addProperty("pl", "Skrzynia została pomyślnie zablokowana!"); // Polonês (PL)
                lockSuccessMessage.addProperty("sv", "Bröstet är framgångsrikt låst!"); // Sueco (SV)
                lockSuccessMessage.addProperty("cs", "Truhla byla úspěšně uzamčena!"); // Tcheco (CS)
                lockSuccessMessage.addProperty("hu", "A láda sikeresen lezárva!"); // Húngaro (HU)
                lockSuccessMessage.addProperty("tr", "Sandık başarıyla kilitlendi!"); // Turco (TR)
                lockSuccessMessage.addProperty("ar", "تم قفل الصندوق بنجاح!"); // Árabe (AR)
                lockSuccessMessage.addProperty("fi", "Arkku lukittu onnistuneesti!"); // Finlandês (FI)
                lockSuccessMessage.addProperty("da", "Kisten blev låst med succes!"); // Dinamarquês (DA)


                JsonObject nameTagReceivedMessage = new JsonObject();
                nameTagReceivedMessage.addProperty("br", "Você recebeu uma etiqueta com a senha."); // Português (BR)
                nameTagReceivedMessage.addProperty("en", "You received a name tag with the password."); // Inglês (EN)
                nameTagReceivedMessage.addProperty("es", "Recibiste una etiqueta con la contraseña."); // Espanhol (ES)
                nameTagReceivedMessage.addProperty("fr", "Vous avez reçu une étiquette avec le mot de passe."); // Francês (FR)
                nameTagReceivedMessage.addProperty("de", "Sie haben ein Namensschild mit dem Passwort erhalten."); // Alemão (DE)
                nameTagReceivedMessage.addProperty("ru", "Вы получили идентификатор с паролем."); // Russo (RU)
                nameTagReceivedMessage.addProperty("zh", "您收到一个带有密码的名称标签。"); // Chinês Simplificado (ZH)
                nameTagReceivedMessage.addProperty("zh-tw", "您收到一個帶有密碼的名稱標籤。"); // Chinês Tradicional (ZH-TW)
                nameTagReceivedMessage.addProperty("ja", "パスワード付きの名前タグを受け取りました。"); // Japonês (JA)
                nameTagReceivedMessage.addProperty("ko", "비밀번호가 포함된 이름 태그를 받았습니다."); // Coreano (KO)
                nameTagReceivedMessage.addProperty("it", "Hai ricevuto un'etichetta con la password."); // Italiano (IT)
                nameTagReceivedMessage.addProperty("nl", "Je hebt een naamtag met het wachtwoord ontvangen."); // Holandês (NL)
                nameTagReceivedMessage.addProperty("pl", "Otrzymałeś etykietę z hasłem."); // Polonês (PL)
                nameTagReceivedMessage.addProperty("sv", "Du har fått en namntag med lösenordet."); // Sueco (SV)
                nameTagReceivedMessage.addProperty("cs", "Obdrželi jste jmenovku s heslem."); // Tcheco (CS)
                nameTagReceivedMessage.addProperty("hu", "Kapott egy névcímkét a jelszóval."); // Húngaro (HU)
                nameTagReceivedMessage.addProperty("tr", "Şifre ile bir isim etiketi aldınız."); // Turco (TR)
                nameTagReceivedMessage.addProperty("ar", "لقد تلقيت بطاقة اسم بها كلمة المرور."); // Árabe (AR)
                nameTagReceivedMessage.addProperty("fi", "Saat nimeämistunnisteen salasanalla varustettuna."); // Finlandês (FI)
                nameTagReceivedMessage.addProperty("da", "Du modtog en navneskilt med adgangskoden."); // Dinamarquês (DA)


                JsonObject providePasswordMessage = new JsonObject();
                providePasswordMessage.addProperty("br", "Por favor, forneça uma senha."); // Português (BR)
                providePasswordMessage.addProperty("en", "Please provide a password."); // Inglês (EN)
                providePasswordMessage.addProperty("es", "Por favor, proporcione una contraseña."); // Espanhol (ES)
                providePasswordMessage.addProperty("fr", "Veuillez fournir un mot de passe."); // Francês (FR)
                providePasswordMessage.addProperty("de", "Bitte geben Sie ein Passwort an."); // Alemão (DE)
                providePasswordMessage.addProperty("ru", "Пожалуйста, введите пароль."); // Russo (RU)
                providePasswordMessage.addProperty("zh", "请提供密码。"); // Chinês Simplificado (ZH)
                providePasswordMessage.addProperty("zh-tw", "請提供密碼。"); // Chinês Tradicional (ZH-TW)
                providePasswordMessage.addProperty("ja", "パスワードを入力してください。"); // Japonês (JA)
                providePasswordMessage.addProperty("ko", "비밀번호를 입력하세요."); // Coreano (KO)
                providePasswordMessage.addProperty("it", "Si prega di fornire una password."); // Italiano (IT)
                providePasswordMessage.addProperty("nl", "Gelieve een wachtwoord op te geven."); // Holandês (NL)
                providePasswordMessage.addProperty("pl", "Proszę podać hasło."); // Polonês (PL)
                providePasswordMessage.addProperty("sv", "Ange ett lösenord."); // Sueco (SV)
                providePasswordMessage.addProperty("cs", "Zadejte prosím heslo."); // Tcheco (CS)
                providePasswordMessage.addProperty("hu", "Kérjük, adjon meg egy jelszót."); // Húngaro (HU)
                providePasswordMessage.addProperty("tr", "Lütfen bir şifre sağlayın."); // Turco (TR)
                providePasswordMessage.addProperty("ar", "يرجى تقديم كلمة المرور."); // Árabe (AR)
                providePasswordMessage.addProperty("fi", "Anna salasana."); // Finlandês (FI)
                providePasswordMessage.addProperty("da", "Angiv venligst en adgangskode."); // Dinamarquês (DA)


                JsonObject lookAtChestMessage = new JsonObject();
                lookAtChestMessage.addProperty("br", "Olhe para um baú para trancá-lo, destrancá-lo ou ver a senha."); // Português (BR)
                lookAtChestMessage.addProperty("en", "Look at a chest to lock, unlock, or view its password."); // Inglês (EN)
                lookAtChestMessage.addProperty("es", "Mira un cofre para bloquearlo, desbloquearlo o ver su contraseña."); // Espanhol (ES)
                lookAtChestMessage.addProperty("fr", "Regardez un coffre pour le verrouiller, le déverrouiller ou voir son mot de passe."); // Francês (FR)
                lookAtChestMessage.addProperty("de", "Schauen Sie sich eine Truhe an, um sie zu sperren, zu entsperren oder ihr Passwort anzuzeigen."); // Alemão (DE)
                lookAtChestMessage.addProperty("ru", "Посмотрите на сундук, чтобы запереть, открыть или увидеть его пароль."); // Russo (RU)
                lookAtChestMessage.addProperty("zh", "查看箱子以锁定、解锁或查看其密码。"); // Chinês Simplificado (ZH)
                lookAtChestMessage.addProperty("zh-tw", "查看箱子以鎖定、解鎖或查看其密碼。"); // Chinês Tradicional (ZH-TW)
                lookAtChestMessage.addProperty("ja", "チェストを見てロック、ロック解除、またはパスワードを確認してください。"); // Japonês (JA)
                lookAtChestMessage.addProperty("ko", "상자를 보고 잠그거나 잠금 해제하거나 비밀번호를 확인하세요."); // Coreano (KO)
                lookAtChestMessage.addProperty("it", "Guarda un baule per bloccarlo, sbloccarlo o visualizzare la sua password."); // Italiano (IT)
                lookAtChestMessage.addProperty("nl", "Kijk naar een kist om deze te vergrendelen, ontgrendelen of het wachtwoord te bekijken."); // Holandês (NL)
                lookAtChestMessage.addProperty("pl", "Spójrz na skrzynię, aby ją zablokować, odblokować lub zobaczyć hasło."); // Polonês (PL)
                lookAtChestMessage.addProperty("sv", "Titta på en kista för att låsa, låsa upp eller se dess lösenord."); // Sueco (SV)
                lookAtChestMessage.addProperty("cs", "Podívejte se na truhlu, abyste ji zamkli, odemkli nebo zobrazili její heslo."); // Tcheco (CS)
                lookAtChestMessage.addProperty("hu", "Nézzen egy ládára, hogy lezárja, kinyissa, vagy megtekintse a jelszavát."); // Húngaro (HU)
                lookAtChestMessage.addProperty("tr", "Bir sandığa bakarak kilitleyin, kilidini açın veya şifresini görün."); // Turco (TR)
                lookAtChestMessage.addProperty("ar", "انظر إلى صندوق لقفله أو فتحه أو عرض كلمة مروره."); // Árabe (AR)
                lookAtChestMessage.addProperty("fi", "Katso arkkua lukitaksesi sen, avataksesi sen tai nähdäksesi salasanan."); // Finlandês (FI)
                lookAtChestMessage.addProperty("da", "Se på en kiste for at låse den, låse den op eller se dens adgangskode."); // Dinamarquês (DA)


                JsonObject chestPasswordMessage = new JsonObject();
                chestPasswordMessage.addProperty("br", "A senha deste baú é: {password}."); // Português (BR)
                chestPasswordMessage.addProperty("en", "The password for this chest is: {password}."); // Inglês (EN)
                chestPasswordMessage.addProperty("es", "La contraseña de este cofre es: {password}."); // Espanhol (ES)
                chestPasswordMessage.addProperty("fr", "Le mot de passe de ce coffre est : {password}."); // Francês (FR)
                chestPasswordMessage.addProperty("de", "Das Passwort für diese Truhe lautet: {password}."); // Alemão (DE)
                chestPasswordMessage.addProperty("ru", "Пароль для этого сундука: {password}."); // Russo (RU)
                chestPasswordMessage.addProperty("zh", "此箱子的密码是：{password}。"); // Chinês Simplificado (ZH)
                chestPasswordMessage.addProperty("zh-tw", "此箱子的密碼是：{password}。"); // Chinês Tradicional (ZH-TW)
                chestPasswordMessage.addProperty("ja", "このチェストのパスワードは: {password} です。"); // Japonês (JA)
                chestPasswordMessage.addProperty("ko", "이 상자의 비밀번호는 {password}입니다."); // Coreano (KO)
                chestPasswordMessage.addProperty("it", "La password per questo baule è: {password}."); // Italiano (IT)
                chestPasswordMessage.addProperty("nl", "Het wachtwoord voor deze kist is: {password}."); // Holandês (NL)
                chestPasswordMessage.addProperty("pl", "Hasło do tej skrzyni to: {password}."); // Polonês (PL)
                chestPasswordMessage.addProperty("sv", "Lösenordet för denna kista är: {password}."); // Sueco (SV)
                chestPasswordMessage.addProperty("cs", "Heslo pro tuto truhlu je: {password}."); // Tcheco (CS)
                chestPasswordMessage.addProperty("hu", "Ennek a ládának a jelszava: {password}."); // Húngaro (HU)
                chestPasswordMessage.addProperty("tr", "Bu sandığın şifresi: {password}."); // Turco (TR)
                chestPasswordMessage.addProperty("ar", "كلمة مرور هذا الصندوق هي: {password}."); // Árabe (AR)
                chestPasswordMessage.addProperty("fi", "Tämän arkun salasana on: {password}."); // Finlandês (FI)
                chestPasswordMessage.addProperty("da", "Adgangskoden til denne kiste er: {password}."); // Dinamarquês (DA)


                JsonObject chestNotLockedMessage = new JsonObject();
                chestNotLockedMessage.addProperty("br", "Este baú não está trancado."); // Português (BR)
                chestNotLockedMessage.addProperty("en", "This chest is not locked."); // Inglês (EN)
                chestNotLockedMessage.addProperty("es", "Este cofre no está bloqueado."); // Espanhol (ES)
                chestNotLockedMessage.addProperty("fr", "Ce coffre n'est pas verrouillé."); // Francês (FR)
                chestNotLockedMessage.addProperty("de", "Diese Truhe ist nicht gesperrt."); // Alemão (DE)
                chestNotLockedMessage.addProperty("ru", "Этот сундук не заперт."); // Russo (RU)
                chestNotLockedMessage.addProperty("zh", "此箱子未上锁。"); // Chinês Simplificado (ZH)
                chestNotLockedMessage.addProperty("zh-tw", "此箱子未上鎖。"); // Chinês Tradicional (ZH-TW)
                chestNotLockedMessage.addProperty("ja", "このチェストはロックされていません。"); // Japonês (JA)
                chestNotLockedMessage.addProperty("ko", "이 상자는 잠겨 있지 않습니다."); // Coreano (KO)
                chestNotLockedMessage.addProperty("it", "Questo baule non è bloccato."); // Italiano (IT)
                chestNotLockedMessage.addProperty("nl", "Deze kist is niet vergrendeld."); // Holandês (NL)
                chestNotLockedMessage.addProperty("pl", "Ta skrzynia nie jest zablokowana."); // Polonês (PL)
                chestNotLockedMessage.addProperty("sv", "Denna kista är inte låst."); // Sueco (SV)
                chestNotLockedMessage.addProperty("cs", "Tato truhla není zamčená."); // Tcheco (CS)
                chestNotLockedMessage.addProperty("hu", "Ez a láda nincs lezárva."); // Húngaro (HU)
                chestNotLockedMessage.addProperty("tr", "Bu sandık kilitli değil."); // Turco (TR)
                chestNotLockedMessage.addProperty("ar", "هذا الصندوق غير مقفل."); // Árabe (AR)
                chestNotLockedMessage.addProperty("fi", "Tämä arkku ei ole lukittu."); // Finlandês (FI)
                chestNotLockedMessage.addProperty("da", "Denne kiste er ikke låst."); // Dinamarquês (DA)


                messages.add("provide_password", providePasswordMessage);
                messages.add("look_at_chest", lookAtChestMessage);
                messages.add("chest_password", chestPasswordMessage);
                messages.add("chest_not_locked", chestNotLockedMessage);
                messages.add("block_break_denied", blockBreakMessage);
                messages.add("lock_success", lockSuccessMessage);
                messages.add("name_tag_received", nameTagReceivedMessage);
                messages.add("lock_chest", lockMessages);
                messages.add("unlock_chest", unlockMessages);
                messages.add("incorrect_password", incorrectPasswordMessage);
                messages.add("locked_chest", lockedChestMessage);
                messages.add("relock_chest", relockChestMessage);
                messages.add("unlocked_temp", unlockedTempMessage);

                // Gson com Pretty Printing
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(messages, writer);

                getLogger().info("Arquivo messages.json criado com mensagens padrão.");
            } catch (IOException e) {
                getLogger().severe("Erro ao criar messages.json: " + e.getMessage());
            }
        }
    }

    private boolean loadPluginStatus() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            JsonObject config = new Gson().fromJson(content, JsonObject.class);
            return config.get("enabled").getAsBoolean();
        } catch (IOException e) {
            getLogger().severe("Erro ao ler o arquivo config.json: " + e.getMessage());
        }
        return false; // Desabilita o plugin em caso de erro
    }

    public String loadLanguage() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                JsonObject config = new Gson().fromJson(content, JsonObject.class);
                return config.get("language").getAsString();
            } catch (IOException e) {
                getLogger().severe("Erro ao ler o arquivo config.json: " + e.getMessage());
            }
        }
        return "br";
    }

    private void setupDatabaseConnection() {
        String DB_URL = "jdbc:sqlite:" + FOLDER_PATH + "/blockdata.db";
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA busy_timeout = 3000;");
            getLogger().info("Conexão com o banco de dados estabelecida.");
        } catch (Exception e) {
            getLogger().severe("Erro ao conectar ao banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS locked_chests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "location TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "player TEXT NOT NULL)";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            getLogger().info("Tabela 'locked_chests' configurada com sucesso!");
        } catch (Exception e) {
            getLogger().severe("Erro ao configurar a tabela 'locked_chests': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void loadLockedChests() {
        String sql = "SELECT location, password FROM locked_chests";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String location = rs.getString("location");
                String password = rs.getString("password");
                lockedChests.put(location, password); // Map now works correctly
            }

        } catch (Exception e) {
            System.err.println("Erro ao carregar os baús: " + e.getMessage());
        }
    }

}
