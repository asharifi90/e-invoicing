@description('Base name used for Azure resources (letters/numbers only recommended).')
param appName string = 'einvoicing'

@description('Azure region.')
param location string = resourceGroup().location

@description('Java web app runtime stack.')
param javaVersion string = 'Java 21'

param appServicePlanSku string = 'F1'

var webAppName = '${appName}-api'
var planName = '${appName}-plan'

resource plan 'Microsoft.Web/serverfarms@2023-12-01' = {
  name: planName
  location: location
  sku: {
    name: appServicePlanSku
  }
  kind: 'linux'
  properties: {
    reserved: true
  }
}

resource webApp 'Microsoft.Web/sites@2023-12-01' = {
  name: webAppName
  location: location
  properties: {
    serverFarmId: plan.id
    httpsOnly: true
    siteConfig: {
      linuxFxVersion: javaVersion
      alwaysOn: false
      appSettings: [
        {
          name: 'SPRING_PROFILES_ACTIVE'
          value: 'azure'
        }
        {
          name: 'SERVER_PORT'
          value: '80'
        }
        {
          name: 'SPRING_DATASOURCE_URL'
          value: 'jdbc:postgresql://REPLACE_HOST:5432/einvoice'
        }
        {
          name: 'SPRING_DATASOURCE_USERNAME'
          value: 'REPLACE_USER'
        }
        {
          name: 'SPRING_KAFKA_BOOTSTRAP_SERVERS'
          value: 'REPLACE_OR_LEAVE_LOCAL'
        }
      ]
    }
  }
}

output webAppName string = webApp.name
output webAppDefaultHostname string = webApp.properties.defaultHostName
