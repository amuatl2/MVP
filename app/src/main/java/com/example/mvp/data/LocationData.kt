package com.example.mvp.data

object LocationData {
    val states = listOf(
        "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
        "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
        "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan",
        "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
        "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
        "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
        "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia",
        "Wisconsin", "Wyoming"
    )
    
    val citiesByState = mapOf(
        "Alabama" to listOf("Birmingham", "Montgomery", "Mobile", "Huntsville", "Tuscaloosa"),
        "Alaska" to listOf("Anchorage", "Fairbanks", "Juneau", "Sitka", "Ketchikan"),
        "Arizona" to listOf("Phoenix", "Tucson", "Mesa", "Chandler", "Scottsdale"),
        "Arkansas" to listOf("Little Rock", "Fort Smith", "Fayetteville", "Springdale", "Jonesboro"),
        "California" to listOf("Los Angeles", "San Diego", "San Jose", "San Francisco", "Fresno", "Sacramento", "Long Beach", "Oakland", "Bakersfield", "Anaheim", "Santa Ana", "Riverside", "Stockton", "Irvine", "Chula Vista", "Fremont", "San Bernardino", "Modesto", "Fontana", "Oxnard"),
        "Colorado" to listOf("Denver", "Colorado Springs", "Aurora", "Fort Collins", "Lakewood"),
        "Connecticut" to listOf("Bridgeport", "New Haven", "Hartford", "Stamford", "Waterbury"),
        "Delaware" to listOf("Wilmington", "Dover", "Newark", "Middletown", "Smyrna"),
        "Florida" to listOf("Jacksonville", "Miami", "Tampa", "Orlando", "St. Petersburg", "Hialeah", "Tallahassee", "Fort Lauderdale", "Port St. Lucie", "Cape Coral"),
        "Georgia" to listOf("Atlanta", "Augusta", "Columbus", "Savannah", "Athens"),
        "Hawaii" to listOf("Honolulu", "Hilo", "Kailua", "Kaneohe", "Kahului"),
        "Idaho" to listOf("Boise", "Nampa", "Meridian", "Idaho Falls", "Pocatello"),
        "Illinois" to listOf("Chicago", "Aurora", "Naperville", "Joliet", "Rockford", "Elgin", "Peoria", "Champaign", "Waukegan", "Cicero"),
        "Indiana" to listOf("Indianapolis", "Fort Wayne", "Evansville", "South Bend", "Carmel"),
        "Iowa" to listOf("Des Moines", "Cedar Rapids", "Davenport", "Sioux City", "Iowa City"),
        "Kansas" to listOf("Wichita", "Overland Park", "Kansas City", "Olathe", "Topeka"),
        "Kentucky" to listOf("Louisville", "Lexington", "Bowling Green", "Owensboro", "Covington"),
        "Louisiana" to listOf("New Orleans", "Baton Rouge", "Shreveport", "Lafayette", "Lake Charles"),
        "Maine" to listOf("Portland", "Lewiston", "Bangor", "South Portland", "Auburn"),
        "Maryland" to listOf("Baltimore", "Frederick", "Rockville", "Gaithersburg", "Bowie"),
        "Massachusetts" to listOf("Boston", "Worcester", "Springfield", "Lowell", "Cambridge"),
        "Michigan" to listOf("Detroit", "Grand Rapids", "Warren", "Sterling Heights", "Lansing"),
        "Minnesota" to listOf("Minneapolis", "St. Paul", "Rochester", "Duluth", "Bloomington"),
        "Mississippi" to listOf("Jackson", "Gulfport", "Southaven", "Hattiesburg", "Biloxi"),
        "Missouri" to listOf("Kansas City", "St. Louis", "Springfield", "Columbia", "Independence"),
        "Montana" to listOf("Billings", "Missoula", "Great Falls", "Bozeman", "Butte"),
        "Nebraska" to listOf("Omaha", "Lincoln", "Bellevue", "Grand Island", "Kearney"),
        "Nevada" to listOf("Las Vegas", "Henderson", "Reno", "North Las Vegas", "Sparks"),
        "New Hampshire" to listOf("Manchester", "Nashua", "Concord", "Derry", "Rochester"),
        "New Jersey" to listOf("Newark", "Jersey City", "Paterson", "Elizabeth", "Edison"),
        "New Mexico" to listOf("Albuquerque", "Las Cruces", "Rio Rancho", "Santa Fe", "Roswell"),
        "New York" to listOf("New York City", "Buffalo", "Rochester", "Yonkers", "Syracuse", "Albany", "New Rochelle", "Mount Vernon", "Schenectady", "Utica"),
        "North Carolina" to listOf("Charlotte", "Raleigh", "Greensboro", "Durham", "Winston-Salem"),
        "North Dakota" to listOf("Fargo", "Bismarck", "Grand Forks", "Minot", "West Fargo"),
        "Ohio" to listOf("Columbus", "Cleveland", "Cincinnati", "Toledo", "Akron"),
        "Oklahoma" to listOf("Oklahoma City", "Tulsa", "Norman", "Broken Arrow", "Lawton"),
        "Oregon" to listOf("Portland", "Eugene", "Salem", "Gresham", "Hillsboro"),
        "Pennsylvania" to listOf("Philadelphia", "Pittsburgh", "Allentown", "Erie", "Reading"),
        "Rhode Island" to listOf("Providence", "Warwick", "Cranston", "Pawtucket", "East Providence"),
        "South Carolina" to listOf("Charleston", "Columbia", "North Charleston", "Mount Pleasant", "Rock Hill"),
        "South Dakota" to listOf("Sioux Falls", "Rapid City", "Aberdeen", "Brookings", "Watertown"),
        "Tennessee" to listOf("Nashville", "Memphis", "Knoxville", "Chattanooga", "Clarksville"),
        "Texas" to listOf("Houston", "San Antonio", "Dallas", "Austin", "Fort Worth", "El Paso", "Arlington", "Corpus Christi", "Plano", "Laredo"),
        "Utah" to listOf("Salt Lake City", "West Valley City", "Provo", "West Jordan", "Orem"),
        "Vermont" to listOf("Burlington", "Essex", "South Burlington", "Colchester", "Rutland"),
        "Virginia" to listOf("Virginia Beach", "Norfolk", "Chesapeake", "Richmond", "Newport News"),
        "Washington" to listOf("Seattle", "Spokane", "Tacoma", "Vancouver", "Bellevue"),
        "West Virginia" to listOf("Charleston", "Huntington", "Parkersburg", "Morgantown", "Wheeling"),
        "Wisconsin" to listOf("Milwaukee", "Madison", "Green Bay", "Kenosha", "Racine"),
        "Wyoming" to listOf("Cheyenne", "Casper", "Laramie", "Gillette", "Rock Springs")
    )
    
    fun getCitiesForState(state: String): List<String> {
        return citiesByState[state] ?: emptyList()
    }
}

