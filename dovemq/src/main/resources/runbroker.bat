set DOVEMQ_DIR=%cd%

java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=%DOVEMQ_DIR% -Xmx2048M -Dcom.sun.management.jmxremote.port=8746 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Ddovemq.dir=%DOVEMQ_DIR%\server.log -cp %DOVEMQ_DIR%;%DOVEMQ_DIR%\lib\commons-lang-2.5.jar;%DOVEMQ_DIR%\lib\dovemq-1.0-SNAPSHOT.jar;%DOVEMQ_DIR%\lib\dovemq-functional-test-1.0-SNAPSHOT.jar;%DOVEMQ_DIR%\lib\junit-4.8.1.jar;%DOVEMQ_DIR%\lib\log4j-1.2.15.jar;%DOVEMQ_DIR%\lib\netty-3.2.4.Final.jar net.dovemq.broker.driver.DoveMQBrokerDriver
