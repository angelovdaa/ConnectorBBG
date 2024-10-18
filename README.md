# BBG CONNECTOR NEOXAM POC

## Présentation
BBG Connector to make *Reference Data Requests* via BBG SAPI
## Installation

### Using simulator

1) git checkout BBGSimulator
2) mvn clean install
3) Put your query parameters (tickers fields, ex.g. "SPY US EQUITY" "LAST_TRADE;LAST_TRADE_SIZE") in ./run.bat and execute it
4) A CSV file (Result_BBG.csv) will be created in the project directory with BBG response

### Using real BBG Server via SAPI

1) git checkout serverAPI
2) mvn clean install
3) Put your query parameters (api_key host port tickers fields -  ex.g. "MY_API_KEY" "MY_HOSTNAME" "8194" "SPY US EQUITY" "LAST_TRADE;LAST_TRADE_SIZE") in ./run.bat and execute it
4) A CSV file (Result_BBG.csv) will be created in the project directory with BBG response