# BrockTrainParse
TTrainParse is an easy to use program that allows any student who is enlisted at Brockenhurst College to take a print screen of their online timetable @ students.brock.ac.uk/timetable (note, it doesn't have to be cropped to fill the entire screen) and my program completely strip of all information and get each day's lesson start – finish times, to allow seamless integration with a SOAP client I developed that consumes the South Western Railway's public database, manipulating the Departure and Arrival board responses according to lesson times.<br><br>All the user must do is send a screenshot, configure the automatically generated Teacher Names.txt so it's tailored to their teacher / teachers and lesson / lessons and hey, sorted.

# Why?
Initially thought up to diagnose a wide spread issue for all commuters to Brockenhurst College constantly flicking between multiple internet reliant applications and websites to find out when your trains are. With this, none of those are an issue and optimal train times for each individual lesson time are accessable online and offline at a clicks notice.

# The stack
- There is an internet reliant android application that allows easy checking of train times and lesson times from the student's phone along with a SOAP client I developed, which is public, to see when a train departing from station 'A' will arrive at station 'B' at 'C' time with 'D' minutes to spare until time 'E' over @ https://github.com/nathan3882/NBIdealTrains
- Public web service over @ api.nathan3882.me/ttrainparse/ will allow third parties to utilise this data too.

# Core APIs
- Local data storage utilises one of my favourite object serialisation libraries, https://github.com/EsotericSoftware/yamlbeans,
- Optical Character Recognition thanks to Tesseract
- JSON serialisation from JSON-java from @stleary
