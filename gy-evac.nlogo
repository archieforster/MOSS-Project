extensions[
  gis
]

; Road links/connections, mediums agents can travel along
breed [roads road]
roads-own[
  len
  node-1
  node-2
  number-of-agents
]

; Buildings
breed [buildings building]
buildings-own[
  capacity ; Maximum number of people that can occupy this building
  occupants ; Track current number of occupants
  latitude ; Store latitude of the building location
  longitude ; Store longitude of the building location
]

; People
breed [people person]
people-own[
  assigned-building ; The building this person is assigned to
]

to clear
  clear-drawing
  clear-all
end

to setup-world
  clear-all
  ; Load GIS data for roads and buildings
  let roads-dataset gis:load-dataset ".\\Data\\TG_RoadLink.shp"
  let buildings-dataset gis:load-dataset ".\\Data\\TG_Building.shp"

  ; Set world envelope based on data
  gis:set-world-envelope (gis:envelope-union-of (gis:envelope-of roads-dataset) (gis:envelope-of buildings-dataset))
  gis:set-world-envelope [641544 653476 300000 319072]

  ; Draw roads and buildings
  gis:set-drawing-color white
  gis:draw roads-dataset 1

  gis:set-drawing-color grey
  gis:draw buildings-dataset 1

  ; Create road agents from the GIS data
  foreach (gis:feature-list-of roads-dataset) [
    r -> create-roads 1 [
      set len (gis:property-value r "LENGTH")
      set node-1 (gis:property-value r "STARTNODE")
      set node-2 (gis:property-value r "ENDNODE")
      set number-of-agents 0
      set hidden? true

    ]
  ]

  ; Create building agents from GIS data
  foreach (gis:feature-list-of buildings-dataset) [
    b ->
      ; Store building latitude and longitude from GIS data
      let xy gis:location-of gis:centroid-of b
       ifelse length xy = 2 [  ; check if conversion was successful
        create-buildings 1 [

      set capacity 3 ; Set maximum capacity to 3 people per building
      set occupants 0 ; Initialize occupants to 0
        set hidden? true

    ; Convert lat-lon to NetLogo coordinates and move the building there

      let x-cor item 0 xy
      let y-cor item 1 xy
      setxy x-cor y-cor
    ]
    ]
[
      print "Warning: Could not convert coordinates for building"
    ]
  ]
  ; Create up to 100 people and assign them to buildings
  create-people 100 [
    let assigned false
    while [not assigned] [
      ; Choose a random building
      let b one-of buildings
      if [occupants] of b < [capacity] of b [
        ; Assign person to the building
        set assigned-building b
        ask b [
          set occupants occupants + 1 ; Increment occupants in the building
        ]
        set assigned true

        ; Visual appearance settings for each person
        set color blue
        set shape "person" ; NetLogo's built-in "person" shape
        set size 0.5 ; Size adjustment to make them visible

      ]
    ]
  ]
end