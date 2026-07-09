# UI24 Remote — App tela cheia para tablet

Projeto Android (Kotlin) pronto para abrir no **Android Studio**. Ele carrega o
site de controle da mesa UI24 dentro do app, em **tela cheia imersiva de verdade**
(a mesma técnica usada por jogos), resolvendo o problema do navegador não
ficar 100% em tela cheia no tablet Xiaomi.

## O que o app faz

- Abre em **tela cheia imersiva** (esconde barra de status e barra de navegação
  do Android, igual um jogo).
- Na primeira vez que abre, mostra uma tela simples com duas opções:
  - **Conectar na mesa** → digite o IP e ele salva.
  - **Usar versão demo** → carrega a demo oficial da Soundcraft
    (`ui24-software-demo/mixer.html`).
- Da próxima vez que abrir o app, **entra direto na mesa salva**, sem pedir IP de novo.
- Existe um **botão invisível** no canto superior esquerdo da tela: segure
  pressionado por ~2,5 segundos para abrir a opção de **desconectar** e voltar
  para a tela de configuração (só assim dá pra trocar o IP depois de configurado).
- Mantém a tela sempre ligada enquanto o app está aberto (como um painel de controle).
- O botão "voltar" do Android não fecha o app nem sai da mesa — modo kiosk.

## Como gerar o APK 100% online (sem instalar nada) — GitHub Actions

O projeto já vem com um "robô" configurado (`.github/workflows/build-apk.yml`)
que compila o APK sozinho na nuvem. Você só precisa de uma conta grátis no GitHub.

1. Crie uma conta em https://github.com (se ainda não tiver).
2. Clique em **New repository** (repositório novo). Dê um nome, ex: `ui24-remote`.
   Pode deixar como **Private**. Não marque nenhuma opção extra. Clique em **Create repository**.
3. Na página do repositório recém-criado, clique no link **uploading an existing file**
   (ou vá em **Add file → Upload files**).
4. Extraia o `UI24RemoteApp.zip` no seu computador/celular. Depois **arraste a pasta
   inteira `UI24RemoteApp` (com tudo dentro)** para dentro da área de upload do GitHub
   no navegador. Ele mantém as subpastas automaticamente.
5. Role para baixo e clique em **Commit changes** (pode deixar tudo como está, só confirmar).
6. Clique na aba **Actions**, no topo do repositório. Se aparecer um aviso pra
   habilitar Actions, clique em **I understand my workflows, go ahead and enable them**.
7. Você verá um workflow chamado **Build APK** rodando (ou clique em **Run workflow**
   se ele não tiver iniciado sozinho). Aguarde uns 3–5 minutos até aparecer o ✅ verde.
8. Clique em cima da execução concluída → role até **Artifacts**, no final da página →
   baixe **UI24Remote-apk** (vem como `.zip`).
9. Extraia esse zip: dentro tem o `app-debug.apk`. Transfira esse arquivo pro tablet
   (Drive, e-mail, cabo, etc.) e instale (autorizando "fontes desconhecidas" quando pedir).

Pronto — nenhum programa foi instalado no seu computador, tudo rodou nos servidores do GitHub.

## Alternativa: gerar pelo Android Studio (se preferir instalar no PC)

1. Instale o **Android Studio** (gratuito): https://developer.android.com/studio
2. Abra o Android Studio → **Open** → selecione a pasta `UI24RemoteApp` (a pasta
   raiz deste projeto, que contém `settings.gradle`).
3. Aguarde o "Gradle Sync" terminar (ele baixa tudo automaticamente na primeira vez —
   precisa de internet nessa etapa).
4. No menu superior: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Quando terminar, clique em **locate** (ou procure em
   `app/build/outputs/apk/debug/app-debug.apk`).
6. Copie esse `.apk` para o tablet (por cabo, e-mail, Drive, etc.) e instale
   (pode ser preciso permitir "instalar de fontes desconhecidas" nas configurações
   do tablet, já que não veio da Play Store).

## Onde ajustar coisas

- **URL da mesa real**: no arquivo `MainActivity.kt`, o app monta a URL como
  `http://SEU_IP/mixer.html`. Se a mesa da sua marca/modelo usar outro caminho,
  troque a linha `"http://$cleanIp/mixer.html"`.
- **Tempo para segurar o botão escondido**: constante `HOLD_TO_DISCONNECT_MS`
  em `MainActivity.kt` (está em 2500 = 2,5 segundos).
- **Posição do botão escondido**: no `activity_main.xml`, o `View` com
  `id="hiddenDisconnectButton"` — hoje está no canto superior esquerdo
  (`layout_gravity="top|start"`). Pode mover para outro canto se preferir.
- **Nome/ícone do app**: `res/values/strings.xml` (nome) e
  `res/drawable/ic_launcher_foreground.xml` (ícone, é um desenho vetorial simples
  que dá pra editar ou substituir por uma imagem sua).

## Observação importante

O app permite tráfego HTTP sem criptografia (`usesCleartextTraffic="true"`),
porque mesas de som como a UI24 rodam um servidor web local **sem HTTPS**. Isso
é normal e necessário para conseguir abrir `http://IP-DA-MESA/`.
