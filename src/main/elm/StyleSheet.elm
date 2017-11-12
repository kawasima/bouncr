module StyleSheet exposing (Styles(..), styleSheet, Element, Attribute, Variation(..))

import Style exposing (..)
import Style.Color
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
    | Icon
    | Button


styleSheet : StyleSheet
styleSheet =
    Style.styleSheet
        [ style None []
        , style Icon
            [ Style.Color.text Color.grey
            , variation Enable
                [ Style.Color.text Color.green
                ]
            ]
        , style Button
            [ Style.Color.text Color.grey
            , variation Enable
                [ Style.Color.text Color.blue
                ]
            , variation Danger
                [ Style.Color.text Color.red
                ]
            ]
        ]
