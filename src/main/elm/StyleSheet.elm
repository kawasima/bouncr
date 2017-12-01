module StyleSheet exposing (Styles(..), styleSheet, Element, Attribute, Variation(..))

import Style exposing (..)
import Style.Color
import Style.Shadow
import Style.Border
import Color exposing (Color)
import Element


type alias StyleSheet =
    Style.StyleSheet Styles Variation


type alias Element msg =
    Element.Element Styles Variation msg


type Variation
    = Enable
    | Danger


type alias Attribute msg =
    Element.Attribute Variation msg


type Styles
    = None
    | Button
    | UserCard
    | Input


styleSheet : StyleSheet
styleSheet =
    Style.styleSheet
        [ style None []
        , style Button
            [ Style.Color.text <| Color.rgb 181 181 182
            , cursor "pointer"
            , variation Enable
                [ Style.Color.text <| Color.rgb 121 190 230 ]
            , variation Danger
                [ Style.Color.text <| Color.rgb 203 82 55 ]
            ]
        , style UserCard
            [ cursor "pointer"
            , variation Enable
                [ Style.Color.background <| Color.rgb 137 181 112 ]
            ]
        , style Input
            [ Style.Color.text <| Color.rgb 73 80 87
            , Style.Color.border <| Color.rgba 0 0 0 0.15
            , Style.Border.all 1
            , Style.Border.solid
            , Style.Border.rounded 4
            , focus
                [ Style.Color.border <| Color.rgb 128 189 255
                , Style.Shadow.box
                    { offset = ( 0, 0 )
                    , size = 0
                    , blur = 0
                    , color = Color.black
                    }
                ]
            ]
        ]
