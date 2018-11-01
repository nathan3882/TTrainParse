# BrockTrainParse

BrockTrainParse is an easy to use program that allows any Brockenhurst College student to upload his/her college timetable and the program will infer the required day, it's start – finish times, to allow seamless integration with South Western Railway's public database via the use of SOAP / XML requests.<br><br>All the user must do is send a screenshot of the timetable with any amount of external information, configure the automatically generated Teacher Names.txt so it's tailored to their teacher / teachers and lesson / lessons and hey presto.<br><br>

Initially thought up to diagnose a wide spread issue for all commuters to Brockenhurst College constantly flicking between multiple internet reliant applications and websites to find out when your trains are. With this, I’ve utilised different data storage methods to cache all results to allow usage with Or without internet access.<br><br>User configuration utilises one of my favourite object serialisation libraries, https://github.com/EsotericSoftware/yamlbeans

