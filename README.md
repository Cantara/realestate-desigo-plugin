# realestate-desigo-plugin
Read PresentValue and Trends from Desigo BAS api


## Manual Test
Run [ManualPluginFactoryTest](src/test/java/com/github/robindevilliers/welcometohell/manual/ManualPluginFactoryTest.java) in your IDE.

## Build
Run `mvn clean install` from the command line.

## Configuration
```
Desigo.ingestionServices.simulator.enabled=true
```

## Defaults
* When no date is found for when TrendId was last imported. The default is last 30 days.
