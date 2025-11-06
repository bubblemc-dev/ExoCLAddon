ExoCLAddon — ClansLite зелёный GLOW для тиммейтов
=================================================

Команда: **/clan glow**  — включает/выключает зелёную подсветку участников *вашего* клана.
Также доступна альтернатива **/clanglow** (на случай, если нужно протестировать без /clan).

Особенности
- Цвет: зелёный (GREEN).
- Подсветка **персонифицированная**: видите её только вы (реализовано через библиотеку GlowingEntities — без ProtocolLib).
- Никаких вмешательств в команды ClansLite — мы аккуратно перехватываем только подкоманду `/clan glow`.

Зависимости
- Paper 1.21.4+
- ClansLite (софт-зависимость; API 1.6.1).

Сборка
- Maven, Java 21: `mvn -q -e -U package`
- Готовый jar: `target/ExoCLAddon-1.0.0.jar`

Конфиг: `plugins/ExoCLAddon/config.yml`
- messages.toggled-on
- messages.toggled-off
- messages.not-in-clan
- messages.no-clanslite

Персистентность
- Файл `plugins/ExoCLAddon/toggles.yml` хранит UUID игроков, у кого включена подсветка.

Автор: BubbleDev
