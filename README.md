# SparkPoints

Sistem de premium points pentru Spigot/Bukkit 1.8.x - 1.21 (Java 8+).
Optional: PlaceholderAPI (pentru DeluxeMenus).

Creat de **Vladuutz** (Discord: _vladuu_). Compatibil Minecraft 1.8 pana la ultima versiune (Java 8+).

## Build
    mvn clean package
Jar-ul ajunge in `target/SparkPoints-1.0.0.jar` -> pui in `plugins/`.

## Comenzi (aliasuri: /sp /sparkp /points /punctespark /sparkpuncte)
| Comanda | Permisiune |
|---|---|
| /sp | sparkpoints.use |
| /sp balance [jucator] | sparkpoints.others |
| /sp pay <jucator> <suma> | sparkpoints.pay |
| /sp top | sparkpoints.top |
| /sp give / take / set <jucator> <suma> | sparkpoints.admin |
| /sp reload | sparkpoints.admin |

## Placeholdere
    %sparkpoints_points%                    -> 1250
    %sparkpoints_points_formatted%          -> 1.250
    %sparkpoints_top_1_name%                -> nume loc 1
    %sparkpoints_top_1_points%              -> puncte loc 1
    %sparkpoints_top_1_points_formatted%

## Exemplu DeluxeMenus
Exemplu item cumparare (DeluxeMenus):
```yaml
items:
  vip_rank:
    material: DIAMOND
    slot: 13
    display_name: '&b&lVIP Rank'
    lore:
      - '&7Pret: &e500 SparkPoints'
      - '&7Ai: &e%sparkpoints_points_formatted%'
    left_click_requirements:
      requirements:
        enough_points:
          type: '>='
          input: '%sparkpoints_points%'
          output: '500'
      deny_commands:
        - '[message] &cNu ai destule SparkPoints!'
    left_click_commands:
      - '[console] sp take %player_name% 500'
      - '[console] lp user %player_name% parent add vip'
      - '[message] &aAi cumparat VIP!'
```

## API pentru alte plugin-uri
```java
PointsManager pm = SparkPoints.getInstance().getManager();
pm.get(uuid); pm.add(uuid, 100); pm.take(uuid, 50); pm.has(uuid, 10);
```
