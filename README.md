# Evolving Wither Storm (Minecraft 1.8.9 / Forge 11.15.1.2318 / Java 8)

Transforma o Wither numa criatura evolutiva inspirada no Wither Storm. **Blocos absorvidos** dão massa e crescimento
físico contínuo; **mortes causadas por seus projéteis** dão progresso evolutivo (atributos e habilidades).

> **Status honesto:** este código foi escrito sem acesso ao Forge/Gradle (ambiente sem rede), portanto **NÃO foi compilado
> nem testado no jogo**. Pode haver erros de compilação (nomes MCP) ou de comportamento. Use o workflow do GitHub abaixo para
> compilar e me envie o log de erros que eu corrijo.

## Estrutura
```
build.gradle / settings.gradle / .github/workflows/build.yml
src/main/java/com/example/witherstorm/
  WitherStormMod, CommonProxy, StormEvents, CommandStorm
  config/StormConfig            evolution/{EvolutionData,EvolutionFormulas,KillTracker}
  entity/{EntityWitherStorm,EntityStormSkull}
  absorb/{BlockAbsorber,BlockMassRegistry}   ability/AbilityManager
  network/{NetworkHandler,PacketAbsorb}
  client/{ClientProxy,RenderWitherStorm,RenderStormSkull,ModelWitherStorm,StormTextures,RenderUtil}
```
Texturas são geradas por código (sem .png). Se mudar o pacote `com.example.witherstorm`, ajuste também o `@SidedProxy`.

## Importar na IDE e compilar localmente
1. Instale JDK 8 e Gradle 2.14.1 (o ForgeGradle 2.1 exige).
2. `gradle setupDecompWorkspace eclipse` (Eclipse) ou `gradle setupDecompWorkspace idea` (IntelliJ), depois importe.
3. Compilar: `gradle build` → o .jar fica em `build/libs/EvolvingWitherStorm-1.8.9-1.0.0.jar` (não use `-sources`).
4. Rodar no dev: `gradle runClient` / `gradle runServer`.

## Instalar
Com Minecraft 1.8.9 + Forge 1.8.9 (11.15.1.2318), copie o .jar para `.minecraft/mods/`. No servidor, a mesma pasta `mods/`.

## Tutorial: compilar no GitHub
1. Crie um repositório e envie todo o conteúdo desta pasta (incluindo `.github/workflows/build.yml`).
2. Aba **Actions** → workflow **Build mod** (roda a cada push; ou "Run workflow").
3. Ao terminar, abra a execução → **Artifacts** → baixe `EvolvingWitherStorm-jar` (zip com o .jar).
4. Se falhar, abra o log do passo "Compilar" e copie os erros.

## Como testar cada mecânica (mundo criativo/superflat, gamerule mobGriefing true)
- **Aparecer:** `/witherstorm spawn` (op) ou invoque um Wither normal (ele é convertido).
- **Absorção/massa/escala:** observe tentáculos puxando blocos; `/witherstorm info` mostra massa e escala crescendo.
- **Mortes:** solte vacas/zumbis perto; `info` mostra `mortes` e `progresso` subindo só quando o projétil/ataque mata.
- **Atributos/habilidades:** `/witherstorm addprogress 2000` e veja o poder subir; barragem (poder 1,5), tentáculos (2), absorção
  acelerada (2,5), bombardeio (3), longo alcance (5), pulso (6). Limites em `config/witherstorm.cfg`.
- **Persistência:** `info`, saia do mundo, volte, `info` novamente (valores mantidos no NBT).
- **Proteção:** bedrock, command block, portais, spawners e blocos com tile entity não são absorvidos.
- **Multiplayer:** o servidor decide massa/mortes/dano; o cliente só recebe escala, estágio, flags e animações.

## Funcionalidades
Massa por bloco configurável; escala contínua `1 + k·(massa/unidade)^p`; estágios visuais (cabeças 3→5→7, espinhos, núcleo,
até 24 tentáculos); absorção em lote com fila BFS, limite por tick, cache de rejeitados; atribuição única de mortes por UUID;
atributos por fórmulas com clamps; regeneração; 7 habilidades com unlock/cooldown; IA (alvo, alimentação, vagar, anti-travamento);
NBT persistente; rede para animação; configuração completa.

## Limitações conhecidas
- Não compilado/testado (ver acima).
- Barra de vida do 1.8.9 limita-se a 1024: a "vida efetiva" maior vira redução de dano proporcional.
- Hitbox limitada (`maxHitboxScale`) e visual limitado (`maxRenderScale`, padrão 40); acima disso crescem poder, alcance e tentáculos, não o modelo.
  A massa e o poder continuam acumulando sem estágio final (limite só de segurança numérica `maxMass`).
- A entidade usa `noClip` (atravessa terreno, que ela mesma absorve) para evitar colisão gigante.
- Modelo feito de caixas procedurais: aparência aproximada do Wither Storm, sem arte detalhada.
- Wither vanilla já salvo no mundo pode não ser convertido; só os que entram via spawn/invocação.
- Cliente e servidor usam seus próprios arquivos de config (a hitbox no cliente pode divergir do servidor).
