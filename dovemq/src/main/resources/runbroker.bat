set DOVEMQ_DIR=%cd%
set JMX_PORT=8746

java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=%DOVEMQ_DIR% -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -Xss256k -Xmx2048M -Xms1536M -Dcom.sun.management.jmxremote.port=%JMX_PORT% -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Ddovemq.log=%DOVEMQ_DIR%\broker.log -cp %DOVEMQ_DIR%;%DOVEMQ_DIR%\lib\commons-lang-2.5.jar;%DOVEMQ_DIR%\lib\dovemq-1.0-SNAPSHOT.jar;%DOVEMQ_DIR%\lib\log4j-1.2.15.jar;%DOVEMQ_DIR%\lib\netty-3.5.8.Final.jar net.dovemq.broker.driver.DoveMQBrokerDriver
