start orbd -ORBInitialPort 1050 -ORBInitialHost localhost
timeout /t 2
start java Diner diner1 true diner5 true diner2
start java Diner diner2 false diner1 true diner3
start java Diner diner3 false diner2 true diner4
start java Diner diner4 false diner3 true diner5
start java Diner diner5 false diner4 false diner1