# BBG CONNECTOR NEOXAM POC

## Présentation
BBG Connector to make *Reference Data Requests* via BBG SAPI
## Installation

### Using SIMULATOR WITHOUT rebuild
git checkout BBGSimulator
1) In *out/Simulator/run.bat* put your query parameters without authentication (tickers fields, ex.g. "SPY US EQUITY" "LAST_TRADE;LAST_TRADE_SIZE")
2) Execute *out/Simulator/run.bat*
3) A CSV file (Result_BBG.csv) will be created in out/ServerAPI with BBG response


### Using SIMULATOR WITH rebuild
git checkout BBGSimulator
1) In BBGConnector directory: mvn clean install
2) In *BBGConnector/run.bat* put your query parameters without authentication (tickers fields, ex.g. "SPY US EQUITY" "LAST_TRADE;LAST_TRADE_SIZE") 
3) Execute *BBGConnector/run.bat*
4) A CSV file (Result_BBG.csv) will be created in out/ServerAPI with BBG response

==============================================================
### Using REAL SAPI WITHOUT rebuild
git checkout serverAPI
1) In *out/ServerAPI/run.bat* put your query parameters with authentication  (api_key host port tickers fields -  ex.g. "MY_API_KEY" "MY_HOSTNAME" "8194" "SPY US EQUITY" "LAST_TRADE;LAST_TRADE_SIZE") in ./run.bat and execute it
2) Execute *out/ServerAPI/run.bat*
3) A CSV file (Result_BBG.csv) will be created in out/ServerAPI with BBG response

### Using real SAPI WITH rebuild
git checkout serverAPI
1) In BBGConnector directory: mvn clean install
2) In *BBGConnector/run.bat* put your query parameters with authentication  (api_key host port tickers fields -  ex.g. "MY_API_KEY" "MY_HOSTNAME" "8194" "SPY US EQUITY" "LAST_TRADE;LAST_TRADE_SIZE") in ./run.bat and execute it
3) Execute *BBGConnector/run.bat*
4) A CSV file (Result_BBG.csv) will be created in out/ServerAPI with BBG response