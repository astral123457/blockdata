# blockdata
Plugin para trancar baú com uma senha e chave Minecraft spigot version 1.21.X Tranca o baú com uma senha Destranca o baú com uma senha Permite ao administrador ver a senha do baú Permite administrador ver a senha caso esqueça

Minecraft command /lock 1233 or /unlok

commands:
  lock:
    description: 'Tranca o baú com uma senha'
    usage: /lock <senha>
  unlock:
    description: 'Destranca o baú com uma senha'
    usage: /unlock <senha>
  viewpassword:
    description: 'Permite ao administrador ver a senha do baú'
    usage: /viewpassword
    permission: viewpassword.use
permissions:
  viewpassword.use:
    description: 'Permite administrador ver a senha caso esqueça'
    default: op

https://www.youtube.com/watch?v=QHVc5ROWEpA
