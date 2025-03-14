# Blockdata
Plugin para trancar baú com uma senha e chave Minecraft spigot version 1.21.X Tranca o baú com uma senha Destranca o baú com uma senha Permite ao administrador ver a senha do baú Permite administrador ver a senha caso esqueça
(obs: se alguem tentar abrirvem fogo e o mesmo que cai na lava!)
Minecraft command /lock 1233

commands:
  lock:
    description: 'Tranca o baú com uma senha'
    usage: /lock <senha>

    
Destranca o baú com uma senha vai precizar da etiqueta com a senha e quebra o bau no ato do destranque 
quem tranca o bau sempre ganha 2 chave 1 para dar ao amigo ou deixa guardada de reserva... 
![image](https://github.com/user-attachments/assets/0f2b775d-00ed-4ce8-bcf1-0a677736c415)

# Inicialização do Plugin:

O plugin inicializa criando os arquivos config.json e messages.json (se não existirem) e configurando o banco de dados para armazenar informações dos baús trancados.

Idioma configurável via o config.json para personalizar mensagens.

# Gerenciamento de Baús Trancados:

Um mapa central (em LockedChests) armazena os baús trancados em memória para rápido acesso.

Atualização do banco de dados para persistência de dados, evitando perda de informações entre reinicializações.

Comandos (lock/unlock/viewpassword):

lock: Tranca um baú com uma senha e cria uma etiqueta com essa senha.

unlock: Destranca um baú ao validar a senha correta.

viewpassword: Permite visualizar a senha de um baú previamente trancado.

# Interações com Eventos:

Eventos como cliques ou destruição de baús são gerenciados para evitar violações (baús trancados não podem ser destruídos sem senha).

Feedback interativo para jogadores com sons, mensagens e efeitos visuais (como fogo ou cabeças de dragão).

# Mensagens Dinâmicas:

Integração com o MessageManager para garantir mensagens localizadas e personalizadas para o idioma do jogador.



https://www.youtube.com/watch?v=QHVc5ROWEpA
