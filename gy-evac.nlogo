extensions[
  gis
  py
]

; Cars, used to move people
breed [cars car]
cars-own[
  id ; Used to reference with python code
  occupants ; Number of people in car
  on-node ; Node car is on
  to-node ; Node car is moving to
]

; Buildings
breed [buildings building]
buildings-own[
  capacity ; Maximum number of people that can occupy this building
  occupants ; Track current number of occupants
  latitude ; Store latitude of the building location
  longitude ; Store longitude of the building location
  nearest-road-node ; Stores nearest road-node for fast lookup
]

; People
breed [people person]
people-own[
  assigned-building ; The building this person is assigned to
  evacuate-now?         ; Track if the person has chosen to evacuate in this period
  evacuating? ; Tracks if person is already evacuating
]

breed [road-nodes road-node]
road-nodes-own[
  id ; Node id
]

globals [
  vehicles-info ; Store information about each car
  total-evacuees ; Track total number of evacuating people
  evacuees-in-transit ; Track number of people currently evacuating
  evacuees-completed ; Track number of people who have completed evacuation
  python-output ; Store Python script output
  vehicle-capacity;
]

to clear
  clear-drawing
  clear-all
end

to setup
  clear ; This does not seem to be working and I am not sure why (makes tick = 0) tried set tick 0 but still did not work.
  reset-evacuation-stats
  setup-world
  clear-ticks
  reset-ticks
end


to reset-evacuation-stats
  set total-evacuees 0
  set evacuees-in-transit 0
  set evacuees-completed 0
  set python-output ""
end

to setup-world
  clear-all
  ; Load GIS data for roads and buildings
  let roads-dataset gis:load-dataset ".\\Data\\SW_RoadLink.shp"
  let buildings-dataset gis:load-dataset ".\\Data\\SW_Building.shp"
  let nodes-dataset gis:load-dataset ".\\Data\\SW_RoadNode.shp"

  ; Set world envelope based on data
  ;gis:set-world-envelope [146728 146895 030800 030951]
  gis:set-world-envelope [144672 150470 027852 032383]

  ; Draw roads and buildings
  gis:set-drawing-color white
  gis:draw roads-dataset 1

  gis:set-drawing-color grey
  gis:draw buildings-dataset 1

  ; Python setup
  py:setup py:python
  py:set "tick_t" tick-time-in-mins
  py:set "over_break_p" over-break-p
  py:set "max_walking_d" max-walking-distance-km
  py:set "terminate_d" terminate-evac-distance-km
  (py:run
    "from path_nav import *"
    "evac_node = '867E0091-5D44-4879-9822-2F810BAED829'"
    "navigator = Navigator(evac_node)"
    "navigator.setMaxWalkingDistance(max_walking_d)"
    "navigator.setTerminateDistance(terminate_d)"
    "set_tick_time_mins(tick_t)"
    "set_over_break_p(over_break_p)"
  )

  ; Create node agents for simpler lookup of nearest nodes to buidlings
  foreach (gis:feature-list-of nodes-dataset)[
  n ->
    let xy gis:location-of gis:centroid-of n
    if length xy = 2 [
      create-road-nodes 1 [
        set id gis:property-value n "IDENTIFIER"
        set xcor item 0 xy
        set ycor item 1 xy
        ifelse id = "867E0091-5D44-4879-9822-2F810BAED829"[
          set hidden? false
          set color red
          set shape "star"
        ][
          set hidden? true
        ]

      ]
      print gis:property-value n "IDENTIFIER"
    ]
  ]

  ; Create building agents from GIS data
  foreach (gis:feature-list-of buildings-dataset) [
    b ->
      let xy gis:location-of gis:centroid-of b
       ifelse length xy = 2 [  ; check if conversion was successful
        create-buildings 1 [
          set capacity 20 ; Set maximum capacity to 3 people per building
          set occupants 0 ; Initialize occupants to 0
          set hidden? true
          let x-cor item 0 xy
          let y-cor item 1 xy
          setxy x-cor y-cor
          ]
        ]
        [
          ; print "Warning: Could not convert coordinates for building"
        ]
  ]

  ; Find & set nearest road node
  foreach sort buildings [
    b ->
    let nearest-road-node-lookup nobody
    let nearest-road-node-dist max-pxcor + max-pycor
    ask road-nodes [
      let dist distance b
      if dist < nearest-road-node-dist[
        set nearest-road-node-lookup self
        set nearest-road-node-dist dist
      ]
    ]
    ask b [set nearest-road-node nearest-road-node-lookup]
  ]

  ; Create a number of people based on the slider value
  create-people initial-people [
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
      ]
    ]

    ; Initially won't evacuate
    set evacuate-now? false
    set evacuating? false

    ; Visual appearance settings for each person
    set color blue
    set shape "person"
    set size 0.5

    ; Move the person to the building's coordinates
    setxy [xcor] of assigned-building [ycor] of assigned-building
  ]
end

to start-evacuation ; Thought seperating start evac and continue evac may make life easier :)
  ; Initialise vehicles-info as an empty list
  ;set vehicles-info []

  ; Reset evacuation statistics
  set evacuees-in-transit 0

  ; Go through each person and set evacuation status based on probability
  ask people with [evacuate-now? = false][
    set evacuate-now? (random-float 1 < evacuation-probability)
    if evacuate-now? = true [
      set hidden? true
    ]
  ]

  ; Init evacuation for people who want to evacuate in same period
  foreach sort buildings[
    b ->
    ; Get evacuating people as those who have chosen to evacuate but haven't already in building
    let evacuating-people people with [evacuate-now? = true and evacuating? = false and assigned-building = b]
    let n-evacuees count evacuating-people
    ; Set evacuees to have evacuating? flag (meaning the system sees them as already evacuating)
    ask evacuating-people [
      set evacuating? true
    ]
    py:set "vehicle_capacity" 5
    if n-evacuees > 0 [
      ; Init all vehicles
      ; IF start node is the evacuation point, then initVehicles will not instantiate new vehicle and evacuees
      py:set "start_node" [id] of [nearest-road-node] of b
      py:set "num_evacuees" n-evacuees
      py:run "navigator.initVehicles(num_evacuees, start_node)"
      ;py:run "print('Evacuating',num_evacuees,'from',start_node)"
    ]
  ]

;  ; Update Python output with initial evacuation statistics
;  set python-output (word python-output "\nInitial Evacuation Statistics:"
;    "\nEvacuation Probability: " evacuation-probability
;    "\nTotal Evacuees: " total-evacuees
;    "\nEvacuees in Transit: " evacuees-in-transit)
end

; Updates each vehicle's movement --> no graphics
;to evacuate-step
;  ; Make a copy of vehicles-info to avoid modifying it during iteration
;  let current-vehicles vehicles-info
;
;  ; Update cars through python
;  py:run "navigator.updateCars()"
;  let finished-vehicles py:run "navigator.getCarsFinishedInUpdate()"

;  ; Only proceed if there are vehicles to process
;  if is-list? current-vehicles and not empty? current-vehicles [
;    foreach current-vehicles [
;      vehicle ->
;      if is-list? vehicle and length vehicle >= 3 [
;        let vehicle-id item 0 vehicle
;        let current-node item 1 vehicle
;        let evacuee item 2 vehicle
;
;        ; Request the next node from Python for this vehicle
;        py:set "car_id" vehicle-id
;        let next-node py:runresult "navigator.popCarNextNode(car_id)"
;
;        ; Move the evacuee associated with this vehicle if there is a next node
;        ifelse next-node != nobody [
;          ask evacuee [
;            setxy [xcor] of next-node [ycor] of next-node
;          ]
;          ; Update vehicle information with the new node
;          let updated-vehicle (list vehicle-id next-node evacuee)
;          set vehicles-info replace-item (position vehicle vehicles-info) vehicles-info updated-vehicle
;
;          ; Get updated Python vehicle state
;          let python-car-info py:runresult "navigator.car_states[car_id]"
;          set python-output (word python-output "\nVehicle Update:"
;            "\nVehicle ID: " vehicle-id
;            "\nNew Node: " next-node
;            "\nVehicle State: " python-car-info)
;        ]
;        [
;          ; If no next node, evacuation is complete for this vehicle
;          set evacuees-completed evacuees-completed + 1
;          set evacuees-in-transit evacuees-in-transit - 1
;          ; Remove the completed vehicle from vehicles-info
;          set vehicles-info remove vehicle vehicles-info
;        ]
;      ]
;    ]
;  ]
;
;  ; Update Python output with current evacuation statistics
;  set python-output (word python-output "\nCurrent Evacuation Statistics:"
;    "\nEvacuation Probability: " evacuation-probability
;    "\nTotal Evacuees: " total-evacuees
;    "\nEvacuees in Transit: " evacuees-in-transit
;    "\nEvacuees Completed: " evacuees-completed)
;end

to go
  ;start-evacuation
  if (ticks mod floor (warning-interval-time-mins / tick-time-in-mins) = 0) [
    start-evacuation
  ]

  py:set "tick_count_nlogo" ticks
  py:run "navigator.updateVehicles(tick_count_nlogo)"

  ; Add termination conditions
  if get-no-evacuating = 0 and count people with [evacuate-now? = false] = 0 [
    print "Evacuation complete! Simulation stopping..."

    py:run (word "navigator.exportJourneyMetrics("
      initial-people ", "
      evacuation-probability ", "
      tick-time-in-mins ", "
      warning-interval-time-mins
      ")"
    )

    stop
  ]

  tick
end

; Output evacuation statistics
to-report get-evacuation-stats
  report (word "Evacuation Statistics:\n"
    "Evacuation Probability: " evacuation-probability "\n"
    "Total Evacuees: " total-evacuees "\n"
    "Evacuees in Transit: " evacuees-in-transit "\n"
    "Evacuees Completed: " evacuees-completed "\n"
    "Python Output:\n" python-output)
end

to-report get-no-active-cars
  report py:runresult "navigator.getNoActiveCars()"
end

to-report get-no-walking
  report py:runresult "navigator.getNoWalking()"
end

to-report get-no-evacuating
  report py:runresult "navigator.getNoEvacuating()"
end

to-report get-no-evacuated
  report py:runresult "navigator.getNoEvacuated()"
end

to-report get-no-in-cars
  report py:runresult "navigator.getNoInCars()"
end

to-report get-avg-no-people-per-car
  report py:runresult "navigator.getAvgNoPeoplePerCar()"
end
@#$#@#$#@
GRAPHICS-WINDOW
210
10
781
582
-1
-1
17.061
1
10
1
1
1
0
1
1
1
-16
16
-16
16
0
0
1
ticks
30.0

BUTTON
50
64
113
97
Setup
setup-world
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
52
114
115
147
Clear
clear
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

INPUTBOX
803
29
958
89
initial-people
15000.0
1
0
Number

SLIDER
799
134
971
167
evacuation-probability
evacuation-probability
0
1
0.17
0.01
1
NIL
HORIZONTAL

BUTTON
47
165
121
198
Go
go
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
0

BUTTON
54
254
140
287
NIL
reset-ticks
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
802
179
974
212
over-break-p
over-break-p
0
1
0.06
.01
1
NIL
HORIZONTAL

INPUTBOX
1059
139
1159
199
tick-time-in-mins
0.25
1
0
Number

MONITOR
1163
36
1298
81
No Evacuating People
get-no-evacuating
0
1
11

MONITOR
1306
38
1409
83
N.o. Active Cars
get-no-active-cars
0
1
11

INPUTBOX
1185
141
1327
201
warning-interval-time-mins
30.0
1
0
Number

PLOT
800
252
1107
496
N.o. People Evacuating
t
N.o. People Evacuating
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"no-in-cars-pen" 1.0 0 -13345367 true "" "plot get-no-in-cars"
"no-walking-pen" 1.0 0 -10899396 true "" "plot get-no-walking"

MONITOR
1172
88
1245
133
Evacuated
get-no-evacuated
0
1
11

INPUTBOX
1185
207
1328
267
max-walking-distance-km
5.0
1
0
Number

INPUTBOX
1185
275
1340
335
terminate-evac-distance-km
0.3
1
0
Number

@#$#@#$#@
## WHAT IS IT?

(a general understanding of what the model is trying to show or explain)

## HOW IT WORKS

(what rules the agents use to create the overall behavior of the model)

## HOW TO USE IT

(how to use the model, including a description of each of the items in the Interface tab)

## THINGS TO NOTICE

(suggested things for the user to notice while running the model)

## THINGS TO TRY

(suggested things for the user to try to do (move sliders, switches, etc.) with the model)

## EXTENDING THE MODEL

(suggested things to add or change in the Code tab to make the model more complicated, detailed, accurate, etc.)

## NETLOGO FEATURES

(interesting or unusual features of NetLogo that the model uses, particularly in the Code tab; or where workarounds were needed for missing features)

## RELATED MODELS

(models in the NetLogo Models Library and elsewhere which are of related interest)

## CREDITS AND REFERENCES

(a reference to the model's URL on the web if it has one, as well as any other necessary credits, citations, and links)
@#$#@#$#@
default
true
0
Polygon -7500403 true true 150 5 40 250 150 205 260 250

airplane
true
0
Polygon -7500403 true true 150 0 135 15 120 60 120 105 15 165 15 195 120 180 135 240 105 270 120 285 150 270 180 285 210 270 165 240 180 180 285 195 285 165 180 105 180 60 165 15

arrow
true
0
Polygon -7500403 true true 150 0 0 150 105 150 105 293 195 293 195 150 300 150

box
false
0
Polygon -7500403 true true 150 285 285 225 285 75 150 135
Polygon -7500403 true true 150 135 15 75 150 15 285 75
Polygon -7500403 true true 15 75 15 225 150 285 150 135
Line -16777216 false 150 285 150 135
Line -16777216 false 150 135 15 75
Line -16777216 false 150 135 285 75

bug
true
0
Circle -7500403 true true 96 182 108
Circle -7500403 true true 110 127 80
Circle -7500403 true true 110 75 80
Line -7500403 true 150 100 80 30
Line -7500403 true 150 100 220 30

butterfly
true
0
Polygon -7500403 true true 150 165 209 199 225 225 225 255 195 270 165 255 150 240
Polygon -7500403 true true 150 165 89 198 75 225 75 255 105 270 135 255 150 240
Polygon -7500403 true true 139 148 100 105 55 90 25 90 10 105 10 135 25 180 40 195 85 194 139 163
Polygon -7500403 true true 162 150 200 105 245 90 275 90 290 105 290 135 275 180 260 195 215 195 162 165
Polygon -16777216 true false 150 255 135 225 120 150 135 120 150 105 165 120 180 150 165 225
Circle -16777216 true false 135 90 30
Line -16777216 false 150 105 195 60
Line -16777216 false 150 105 105 60

car
false
0
Polygon -7500403 true true 300 180 279 164 261 144 240 135 226 132 213 106 203 84 185 63 159 50 135 50 75 60 0 150 0 165 0 225 300 225 300 180
Circle -16777216 true false 180 180 90
Circle -16777216 true false 30 180 90
Polygon -16777216 true false 162 80 132 78 134 135 209 135 194 105 189 96 180 89
Circle -7500403 true true 47 195 58
Circle -7500403 true true 195 195 58

circle
false
0
Circle -7500403 true true 0 0 300

circle 2
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240

cow
false
0
Polygon -7500403 true true 200 193 197 249 179 249 177 196 166 187 140 189 93 191 78 179 72 211 49 209 48 181 37 149 25 120 25 89 45 72 103 84 179 75 198 76 252 64 272 81 293 103 285 121 255 121 242 118 224 167
Polygon -7500403 true true 73 210 86 251 62 249 48 208
Polygon -7500403 true true 25 114 16 195 9 204 23 213 25 200 39 123

cylinder
false
0
Circle -7500403 true true 0 0 300

dot
false
0
Circle -7500403 true true 90 90 120

face happy
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 255 90 239 62 213 47 191 67 179 90 203 109 218 150 225 192 218 210 203 227 181 251 194 236 217 212 240

face neutral
false
0
Circle -7500403 true true 8 7 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Rectangle -16777216 true false 60 195 240 225

face sad
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 168 90 184 62 210 47 232 67 244 90 220 109 205 150 198 192 205 210 220 227 242 251 229 236 206 212 183

fish
false
0
Polygon -1 true false 44 131 21 87 15 86 0 120 15 150 0 180 13 214 20 212 45 166
Polygon -1 true false 135 195 119 235 95 218 76 210 46 204 60 165
Polygon -1 true false 75 45 83 77 71 103 86 114 166 78 135 60
Polygon -7500403 true true 30 136 151 77 226 81 280 119 292 146 292 160 287 170 270 195 195 210 151 212 30 166
Circle -16777216 true false 215 106 30

flag
false
0
Rectangle -7500403 true true 60 15 75 300
Polygon -7500403 true true 90 150 270 90 90 30
Line -7500403 true 75 135 90 135
Line -7500403 true 75 45 90 45

flower
false
0
Polygon -10899396 true false 135 120 165 165 180 210 180 240 150 300 165 300 195 240 195 195 165 135
Circle -7500403 true true 85 132 38
Circle -7500403 true true 130 147 38
Circle -7500403 true true 192 85 38
Circle -7500403 true true 85 40 38
Circle -7500403 true true 177 40 38
Circle -7500403 true true 177 132 38
Circle -7500403 true true 70 85 38
Circle -7500403 true true 130 25 38
Circle -7500403 true true 96 51 108
Circle -16777216 true false 113 68 74
Polygon -10899396 true false 189 233 219 188 249 173 279 188 234 218
Polygon -10899396 true false 180 255 150 210 105 210 75 240 135 240

house
false
0
Rectangle -7500403 true true 45 120 255 285
Rectangle -16777216 true false 120 210 180 285
Polygon -7500403 true true 15 120 150 15 285 120
Line -16777216 false 30 120 270 120

leaf
false
0
Polygon -7500403 true true 150 210 135 195 120 210 60 210 30 195 60 180 60 165 15 135 30 120 15 105 40 104 45 90 60 90 90 105 105 120 120 120 105 60 120 60 135 30 150 15 165 30 180 60 195 60 180 120 195 120 210 105 240 90 255 90 263 104 285 105 270 120 285 135 240 165 240 180 270 195 240 210 180 210 165 195
Polygon -7500403 true true 135 195 135 240 120 255 105 255 105 285 135 285 165 240 165 195

line
true
0
Line -7500403 true 150 0 150 300

line half
true
0
Line -7500403 true 150 0 150 150

pentagon
false
0
Polygon -7500403 true true 150 15 15 120 60 285 240 285 285 120

person
false
0
Circle -7500403 true true 110 5 80
Polygon -7500403 true true 105 90 120 195 90 285 105 300 135 300 150 225 165 300 195 300 210 285 180 195 195 90
Rectangle -7500403 true true 127 79 172 94
Polygon -7500403 true true 195 90 240 150 225 180 165 105
Polygon -7500403 true true 105 90 60 150 75 180 135 105

plant
false
0
Rectangle -7500403 true true 135 90 165 300
Polygon -7500403 true true 135 255 90 210 45 195 75 255 135 285
Polygon -7500403 true true 165 255 210 210 255 195 225 255 165 285
Polygon -7500403 true true 135 180 90 135 45 120 75 180 135 210
Polygon -7500403 true true 165 180 165 210 225 180 255 120 210 135
Polygon -7500403 true true 135 105 90 60 45 45 75 105 135 135
Polygon -7500403 true true 165 105 165 135 225 105 255 45 210 60
Polygon -7500403 true true 135 90 120 45 150 15 180 45 165 90

sheep
false
15
Circle -1 true true 203 65 88
Circle -1 true true 70 65 162
Circle -1 true true 150 105 120
Polygon -7500403 true false 218 120 240 165 255 165 278 120
Circle -7500403 true false 214 72 67
Rectangle -1 true true 164 223 179 298
Polygon -1 true true 45 285 30 285 30 240 15 195 45 210
Circle -1 true true 3 83 150
Rectangle -1 true true 65 221 80 296
Polygon -1 true true 195 285 210 285 210 240 240 210 195 210
Polygon -7500403 true false 276 85 285 105 302 99 294 83
Polygon -7500403 true false 219 85 210 105 193 99 201 83

square
false
0
Rectangle -7500403 true true 30 30 270 270

square 2
false
0
Rectangle -7500403 true true 30 30 270 270
Rectangle -16777216 true false 60 60 240 240

star
false
0
Polygon -7500403 true true 151 1 185 108 298 108 207 175 242 282 151 216 59 282 94 175 3 108 116 108

target
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240
Circle -7500403 true true 60 60 180
Circle -16777216 true false 90 90 120
Circle -7500403 true true 120 120 60

tree
false
0
Circle -7500403 true true 118 3 94
Rectangle -6459832 true false 120 195 180 300
Circle -7500403 true true 65 21 108
Circle -7500403 true true 116 41 127
Circle -7500403 true true 45 90 120
Circle -7500403 true true 104 74 152

triangle
false
0
Polygon -7500403 true true 150 30 15 255 285 255

triangle 2
false
0
Polygon -7500403 true true 150 30 15 255 285 255
Polygon -16777216 true false 151 99 225 223 75 224

truck
false
0
Rectangle -7500403 true true 4 45 195 187
Polygon -7500403 true true 296 193 296 150 259 134 244 104 208 104 207 194
Rectangle -1 true false 195 60 195 105
Polygon -16777216 true false 238 112 252 141 219 141 218 112
Circle -16777216 true false 234 174 42
Rectangle -7500403 true true 181 185 214 194
Circle -16777216 true false 144 174 42
Circle -16777216 true false 24 174 42
Circle -7500403 false true 24 174 42
Circle -7500403 false true 144 174 42
Circle -7500403 false true 234 174 42

turtle
true
0
Polygon -10899396 true false 215 204 240 233 246 254 228 266 215 252 193 210
Polygon -10899396 true false 195 90 225 75 245 75 260 89 269 108 261 124 240 105 225 105 210 105
Polygon -10899396 true false 105 90 75 75 55 75 40 89 31 108 39 124 60 105 75 105 90 105
Polygon -10899396 true false 132 85 134 64 107 51 108 17 150 2 192 18 192 52 169 65 172 87
Polygon -10899396 true false 85 204 60 233 54 254 72 266 85 252 107 210
Polygon -7500403 true true 119 75 179 75 209 101 224 135 220 225 175 261 128 261 81 224 74 135 88 99

wheel
false
0
Circle -7500403 true true 3 3 294
Circle -16777216 true false 30 30 240
Line -7500403 true 150 285 150 15
Line -7500403 true 15 150 285 150
Circle -7500403 true true 120 120 60
Line -7500403 true 216 40 79 269
Line -7500403 true 40 84 269 221
Line -7500403 true 40 216 269 79
Line -7500403 true 84 40 221 269

wolf
false
0
Polygon -16777216 true false 253 133 245 131 245 133
Polygon -7500403 true true 2 194 13 197 30 191 38 193 38 205 20 226 20 257 27 265 38 266 40 260 31 253 31 230 60 206 68 198 75 209 66 228 65 243 82 261 84 268 100 267 103 261 77 239 79 231 100 207 98 196 119 201 143 202 160 195 166 210 172 213 173 238 167 251 160 248 154 265 169 264 178 247 186 240 198 260 200 271 217 271 219 262 207 258 195 230 192 198 210 184 227 164 242 144 259 145 284 151 277 141 293 140 299 134 297 127 273 119 270 105
Polygon -7500403 true true -1 195 14 180 36 166 40 153 53 140 82 131 134 133 159 126 188 115 227 108 236 102 238 98 268 86 269 92 281 87 269 103 269 113

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270
@#$#@#$#@
NetLogo 6.4.0
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
default
0.0
-0.2 0 0.0 1.0
0.0 1 1.0 0.0
0.2 0 0.0 1.0
link direction
true
0
Line -7500403 true 150 150 90 180
Line -7500403 true 150 150 210 180
@#$#@#$#@
0
@#$#@#$#@
