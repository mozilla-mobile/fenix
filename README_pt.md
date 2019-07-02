# Firefox Preview

Firefox Preview (nome do código interno: "Fenix") é um is navegador totalmente novo para Android, baseado no [GeckoView](https://mozilla.github.io/geckoview/) e [Mozilla Android Components](https://mozac.org/).

## Participar

Nós encorajamos você a participar deste projeto de código aberto. Nós adoramos Pull Requests, relatórios de bugs, idéias, revisões de código (de segurança) ou qualquer outro tipo de contribuição positiva.
 

Antes de tentar fazer uma contribuição por favor leia o [Diretrizes de Participação da Comunidade](https://www.mozilla.org/en-US/about/governance/policies/participation/).

* [Guia para Contribuir](https://github.com/mozilla-mobile/shared-docs/blob/master/android/CONTRIBUTING.md) (**Novos contribuidores começam aqui!**)

*[Ver atuais Issues](https://github.com/mozilla-mobile/fenix/issues), [ver atuais Pull Requests](https://github.com/mozilla-mobile/fenix/pulls), ou [Apresentar um problema de segurança][sec issue].

* IRC: [#fenix (irc.mozilla.org)](https://wiki.mozilla.org/IRC) | [ver regitros](https://mozilla.logbot.info/fenix/)
(**Estamos disponíveis de segunda a sexta, horário de funcionamento GMT e PST**).

* [Ver o Wiki](https://github.com/mozilla-mobile/fenix/wiki).

**Iniciantes!** - Fiquem atentos para os [Problemas com o rótulo "Good First Issue"](https://github.com/mozilla-mobile/fenix/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22). Estes são bugs fáceis que foram deixados para os novatos se aventurarem, se envolverem e contribuírem positivamente para o projeto.!


## Construir Instruções


1. Clone ou faça Download do repositório:

  ```shell
  git clone https://github.com/mozilla-mobile/fenix
  ```

2. Importe o projeto no Android Studio **ou** faça na linha de comando:

  ```shell
  ./gradlew clean app:assembleArmDebug
  ```

3. Certifique-se de selecionar a variante de compilação correta no Android Studio:
**armDebug** for ARM
**x86Debug** for X86

## Pre-push hooks
Para reduzir o tempo de revisão, gostaríamos que todos os pushes executassem testes localmente. 
Recomendamos que você use nosso pre-push hook fornecido em `config / pre-push-recommended.sh`. 
Usar esse hook garantirá que seu hook seja atualizado conforme o repositório for alterado. 
Este hook tenta rodar o máximo possível sem gastar muito tempo.


Para adicioná-lo no Mac / Linux, execute este comando a partir do projeto root:
```sh
ln -s ../../config/pre-push-recommended.sh .git/hooks/pre-push
```
ou para o Windows executar este comando com privilégios administrativos:
```sh
mklink .git\hooks\pre-push ..\..\config\pre-push-recommended.sh
```

Para fazer o push sem executar o pre-push (por exemplo, atualizações de documentos):
```sh
git push <remote> --no-verify
```

## Licença


    Este Formulário de Código-Fonte está sujeito aos termos da Licença Pública 
    Mozilla, v. 2.0. Se uma cópia da MPL não foi distribuída com este arquivo, 
    você pode obter uma em http://mozilla.org/MPL/2.0/
