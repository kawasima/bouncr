module StyleSheet exposing (Styles(..), styleSheet, Element, Attribute)

import Style exposing (..)
import Style.Color
import Color exposing (Color)
import Element


type alias StyleSheet =
    Style.StyleSheet Styles ()


type alias Element msg =
    Element.Element Styles () msg


type alias Attribute msg =
    Element.Attribute () msg


type Styles
    = None
    | IconChecked
    | IconUnchecked


styleSheet : StyleSheet
styleSheet =
    Style.styleSheet
        [ style None []
        , style IconChecked [ Style.Color.text Color.green ]
        , style IconUnchecked [ Style.Color.text Color.grey ]
        ]
