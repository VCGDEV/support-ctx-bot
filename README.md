# SupportCtxBot
A basic BOT implementation, using:

  - Telegram API [https://github.com/mukel/telegrambot4s](https://github.com/mukel/telegrambot4s)
  - [Wit.Ai](https://wit.ai)
  - Drools rules engine [http://bleibinha.us/blog/2014/04/drools-with-scala](http://bleibinha.us/blog/2014/04/drools-with-scala) 

#Configurations
  ##Telegram configuration
  Add token from @BotFather in src/main/resources/bot.token
  Example:
        
    123312:AAFFpR
  
  ##Wit configuration
  Add your own wit.ai configuration in src/main/resources/application.properties
   
    wit.ai.token=Bearer QQUHMVCCYOX7H5WX7FXLGCA3BMPZH5CP
    wit.ai.url=https://api.wit.ai/message
    wit.ai.version=20180402
    wit.ai.id=5a3c486f-9494-454a-ab44-0f6d7cd78304
  ## Security configuration
  Add your own security oauth implementation if you need access to secured API's in this case, I've an oauth2 server,
  so I've to add this configurations in src/main/resources/application.properties, if you do not need this just remove the **config.oauth.OauthCredentials** object:
   
    application.username=
    application.password=
    application.oauth.grant=
    application.oauth.client-id=
    application.oauth.client-secret=
    application.oauth.url=
    application.rest.base.url=
