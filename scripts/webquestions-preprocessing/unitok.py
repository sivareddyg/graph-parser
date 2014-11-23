#!/usr/bin/env python

"""
Universal tokenizer

This code was highly inspired by Laurent Pointal's TreeTagger wrapper:
http://www.limsi.fr/Individu/pointal/python/treetaggerwrapper.py

Lists of clictics and abbreviations were taken from the TreeTagger:
http://www.ims.uni-stuttgart.de/projekte/corplex/TreeTagger/

(c) 2009 Jan Pomikalek <jan.pomikalek@gmail.com>
"""

import re

# regular expressions
# mostly taken from http://www.limsi.fr/Individu/pointal/python/treetaggerwrapper.py
SGML_TAG = ur"""
    (?:                         # make enclosing parantheses non-grouping
    <!-- .*? -->                # XML/SGML comment
    |                           # -- OR --
    <[!?/]?(?!\d)\w[-\.:\w]*    # Start of tag/directive
    (?:                         # Attributes
        [^>'"]*                 # - attribute name (+whitespace +equal sign)
        (?:'[^']*'|"[^"]*")     # - attribute value
    )* 
    \s*                         # Spaces at the end
    /?                          # Forward slash at the end of singleton tags
    \s*                         # More spaces at the end
    >                           # +End of tag/directive
    )"""
SGML_TAG_RE = re.compile(SGML_TAG, re.UNICODE | re.VERBOSE | re.DOTALL)

SGML_END_TAG = ur"</(?!\d)\w[-\.:\w]*>"
SGML_END_TAG_RE = re.compile(SGML_END_TAG, re.UNICODE)

IP_ADDRESS = ur"(?:[0-9]{1,3}\.){3}[0-9]{1,3}"
IP_ADDRESS_RE = re.compile(IP_ADDRESS)

DNS_HOST = ur"""
    (?:
        [-a-z0-9]+\.                # Host name
        (?:[-a-z0-9]+\.)*           # Intermediate domains
                                    # And top level domain below
        (?:
        com|edu|gov|int|mil|net|org|            # Common historical TLDs
        biz|info|name|pro|aero|coop|museum|     # Added TLDs
        arts|firm|info|nom|rec|shop|web|        # ICANN tests...
        asia|cat|jobs|mail|mobi|post|tel|
        travel|xxx|
        glue|indy|geek|null|oss|parody|bbs|     # OpenNIC
        localdomain|                            # Default 127.0.0.0

        # And here the country TLDs
        ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|
        ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|
        ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|
        de|dj|dk|dm|do|dz|
        ec|ee|eg|eh|er|es|et|
        fi|fj|fk|fm|fo|fr|
        ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|
        hk|hm|hn|hr|ht|hu|
        id|ie|il|im|in|io|iq|ir|is|it|
        je|jm|jo|jp|
        ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|
        la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|
        ma|mc|md|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|
        na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|
        om|
        pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|
        qa|
        re|ro|ru|rw|
        sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|st|sv|sy|sz|
        tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|
        ua|ug|uk|um|us|uy|uz|
        va|vc|ve|vg|vi|vn|vu|
        wf|ws|
        ye|yt|yu|
        za|zm|zw
        )

        |

        localhost
    )"""
DNS_HOST_RE = re.compile(DNS_HOST, re.VERBOSE | re.IGNORECASE)

URL = ur"""
    # Scheme part
    (?:ftp|https?|gopher|mailto|news|nntp|telnet|wais|file|prospero)://

    # User authentication (optional)
    (?:[-a-z0-9_;?&=](?::[-a-z0-9_;?&=]*)?@)?

    # DNS host / IP
    (?:
        """ + DNS_HOST + """
        |
        """ + IP_ADDRESS +"""
    )
    
    # Port specification (optional)
    (?::[0-9]+)?      

    # Scheme specific extension (optional)
    (?:/[-\w;/?:@=&\$_.+!*'(~#%,]*)?
"""
URL_RE = re.compile(URL, re.VERBOSE | re.IGNORECASE | re.UNICODE)

EMAIL = ur"[-a-z0-9._']+@" + DNS_HOST
EMAIL_RE = re.compile(EMAIL, re.VERBOSE | re.IGNORECASE)

# also matches initials
# FIXME! only match capital letters (?)
ACRONYM = ur"""
    (?<!\w)     # should not be preceded by a letter
    # sequence of single letter followed by . (e.g. U.S.)
    (?:
        (?![\d_])\w         # alphabetic character
        \.
    )+
    # optionaly followed by a single letter (e.g. U.S.A)
    (?:
        (?![\d_])\w         # alphabetic character
        (?!\w)              # we don't want any more letters to follow
                            # we only want to match U.S. in U.S.Army (not U.S.A)
    )?
"""
ACRONYM_RE = re.compile(ACRONYM, re.UNICODE | re.VERBOSE)

CONTROL_CHAR = ur"[\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008" \
                ur"\u000B\u000C\u000D\u000E\u000F\u0010\u0011\u0012\u0013" \
                ur"\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C" \
                ur"\u001D\u001E\u001F]"
CONTROL_CHAR_RE = re.compile(CONTROL_CHAR, re.UNICODE)

MULTICHAR_PUNCTUATION = ur"(?:[?!]+|``|'')"
MULTICHAR_PUNCTUATION_RE = re.compile(MULTICHAR_PUNCTUATION, re.VERBOSE)

# These punctuation marks should be tokenised to single characters
# even if a sequence of the same characters is found. For example,
# tokenise '(((' as ['(', '(', '('] rather than ['((('].
OPEN_CLOSE_PUNCTUATION = ur"""
    [
        \u00AB \u2018 \u201C \u2039 \u00BB \u2019 \u201D \u203A \u0028 \u005B
        \u007B \u0F3A \u0F3C \u169B \u2045 \u207D \u208D \u2329 \u23B4 \u2768
        \u276A \u276C \u276E \u2770 \u2772 \u2774 \u27E6 \u27E8 \u27EA \u2983
        \u2985 \u2987 \u2989 \u298B \u298D \u298F \u2991 \u2993 \u2995 \u2997
        \u29D8 \u29DA \u29FC \u3008 \u300A \u300C \u300E \u3010 \u3014 \u3016
        \u3018 \u301A \u301D \uFD3E \uFE35 \uFE37 \uFE39 \uFE3B \uFE3D \uFE3F
        \uFE41 \uFE43 \uFE47 \uFE59 \uFE5B \uFE5D \uFF08 \uFF3B \uFF5B \uFF5F
        \uFF62 \u0029 \u005D \u007D \u0F3B \u0F3D \u169C \u2046 \u207E \u208E
        \u232A \u23B5 \u2769 \u276B \u276D \u276F \u2771 \u2773 \u2775 \u27E7
        \u27E9 \u27EB \u2984 \u2986 \u2988 \u298A \u298C \u298E \u2990 \u2992
        \u2994 \u2996 \u2998 \u29D9 \u29DB \u29FD \u3009 \u300B \u300D \u300F
        \u3011 \u3015 \u3017 \u3019 \u301B \u301E \u301F \uFD3F \uFE36 \uFE38
        \uFE3A \uFE3C \uFE3E \uFE40 \uFE42 \uFE44 \uFE48 \uFE5A \uFE5C \uFE5E
        \uFF09 \uFF3D \uFF5D \uFF60 \uFF63
    ]
"""
OPEN_CLOSE_PUNCTUATION_RE = re.compile(OPEN_CLOSE_PUNCTUATION, re.UNICODE | re.VERBOSE)

PHONE_NUMBER = ur"\+?[0-9]+(?:[-\u2012 ][0-9]+)*"
PHONE_NUMBER_RE = re.compile(PHONE_NUMBER, re.UNICODE)

NUMBER_INTEGER_PART = ur"""
    (?:
        0           
        |
        [1-9][0-9]{0,2}(?:[ ,.][0-9]{3})+  # with thousand separators
        |
        [1-9][0-9]*      
    )"""
NUMBER_DECIMAL_PART = ur"""
    (?:
        [.,]
        [0-9]+
        (?:[eE][-\u2212+]?[0-9]+)?
    )"""
NUMBER = ur"""
    (?:(?:\A|(?<=\s))[-\u2212+])?
    (?:
        %(integer)s %(decimal)s?
        |
        %(decimal)s
    )""" % {'integer': NUMBER_INTEGER_PART, 'decimal': NUMBER_DECIMAL_PART }

NUMBER_RE = re.compile(NUMBER, re.UNICODE | re.VERBOSE)

WORD = ur"(?:(?![\d])[-\u2010\w])+"
WORD_RE = re.compile(WORD, re.UNICODE)

WHITESPACE = ur"\s+"
WHITESPACE_RE = re.compile(WHITESPACE)

SPACE = ur"[\u00A0\u1680\u180E\u2000\u2001\u2002\u2003\u2004\u2005\u2006" \
         ur"\u2007\u2008\u2009\u200A\u202F\u205F\u3000]"
SPACE_RE = re.compile(SPACE, re.UNICODE)

ANY_SEQUENCE = ur"(.)\1*"
ANY_SEQUENCE_RE = re.compile(ANY_SEQUENCE)

HTMLENTITY = ur"&(?:#x?[0-9]+|\w+);"
HTMLENTITY_RE = re.compile(HTMLENTITY)

GLUE_TAG = u'<g/>'

def htmlent2unicode(htmlent, dont_convert=[]):
    import htmlentitydefs
    m = re.match("^&#([0-9]+);$", htmlent)
    if m:
        return unichr(int(m.group(1)))
    m = re.match("^&#x([0-9]+);$", htmlent)
    if m:
        return unichr(int(m.group(1), 16))
    m = re.match("^&(\w+);$", htmlent)
    if m:
        name = m.group(1)
        if name in dont_convert:
            return htmlent
        if htmlentitydefs.name2codepoint.has_key(name):
            return unichr(htmlentitydefs.name2codepoint[name])
        else:
            # return entities with unknown names unchanged
            return htmlent
    raise ValueError, "invalid HTML entity: %s" % htmlent

def robust_htmlent2unicode(htmlent, dont_convert=[]):
    try:
        return htmlent2unicode(htmlent, dont_convert)
    except ValueError:
        return u''

def replace_html_entities(text, exceptions=['gt', 'lt', 'quot']):
    return HTMLENTITY_RE.sub(lambda x: robust_htmlent2unicode(x.group(), exceptions), text)

def tokenise_recursively(text, re_list, depth=0):
    if depth >= len(re_list):
        return [text]
    regular_expr = re_list[depth]
    tokens = []
    pos = 0
    while pos < len(text):
        m = regular_expr.search(text, pos)
        if not m:
            tokens.extend(tokenise_recursively(text[pos:], re_list, depth+1))
            break
        else:
            startpos, endpos = m.span()
            if startpos > pos:
                tokens.extend(tokenise_recursively(text[pos:startpos], re_list, depth+1))
            tokens.append(text[startpos:endpos])
            pos = endpos
    return tokens

class LanguageSpecificData:
    clictics = None
    abbreviations = re.compile(ur"""
(?<!\w)     # should not be preceded by a letter
(?:
    co\.|inc\.|ltd\.|dr\.|prof\.|jr\.
)
""", re.IGNORECASE | re.UNICODE | re.VERBOSE)

def tokenise(text, lsd=LanguageSpecificData(), glue=GLUE_TAG):
    re_list = [
        SGML_TAG_RE,
    ]
    if lsd.abbreviations:
        re_list.append(lsd.abbreviations)
    if lsd.clictics:
        re_list.append(lsd.clictics)
    re_list.extend([
        WHITESPACE_RE,
        URL_RE,
        EMAIL_RE,
        IP_ADDRESS_RE,
#        PHONE_NUMBER_RE,
        HTMLENTITY_RE,
        NUMBER_RE,
        ACRONYM_RE,
        WORD_RE,
        MULTICHAR_PUNCTUATION_RE,
        OPEN_CLOSE_PUNCTUATION_RE,
        ANY_SEQUENCE_RE,
    ])

    text = CONTROL_CHAR_RE.sub("", text)    # remove control chars
    text = replace_html_entities(text)
    text = SPACE_RE.sub(" ", text)          # replace special spaces with normal space
    
    tokens = tokenise_recursively(text, re_list)

    # replace &lt; &gt; and &quot;
    # (cannot be replaced earlier as it would harm SGML tags)
    tmp_tokens = []
    for t in tokens:
        if not SGML_TAG_RE.match(t):
            tmp_tokens.append(replace_html_entities(t, exceptions=[]))
        else:
            tmp_tokens.append(t)
    tokens = tmp_tokens
    
    # replace newlines with spaces
    tokens = [re.sub("[\r\n]", " ", t) for t in tokens]

    glued_tokens = []
    should_add_glue = False
    for token in tokens:
        if WHITESPACE_RE.match(token):
            should_add_glue = False
        elif SGML_END_TAG_RE.match(token):
            glued_tokens.append(token)
        elif SGML_TAG_RE.match(token):
            if should_add_glue and glue is not None:
                glued_tokens.append(glue)
            glued_tokens.append(token)
            should_add_glue = False
        else:
            if should_add_glue and glue is not None:
                glued_tokens.append(glue)
            glued_tokens.append(token)
            should_add_glue = True

    return glued_tokens

### LANGUAGE DATA #############################################################
class EnglishData(LanguageSpecificData):
    def __init__(self):
        self.clictics = re.compile(ur"""
            (?:
                (?<=\w)     # only consider clictics preceded by a letter
                (?:
                    ['\u2019](?:s|re|ve|d|m|em|ll)
                    |
                    n['\u2019]t
                )
                |
                # cannot
                (?<=can)
                not
            )
            (?!\w)          # clictics should not be followed by a letter
            """, re.UNICODE | re.VERBOSE | re.IGNORECASE)

        self.abbreviations = re.compile(ur"""
(?<!\w)     # should not be preceded by a letter
(?:
    'm|'d|'ll|'re|'s|'t|'ve|Adm\.|Ala\.|Ariz\.|Ark\.|Aug\.|Ave\.|Bancorp\.|Bhd\.|Brig\.|
    Bros\.|CO\.|CORP\.|COS\.|Ca\.|Calif\.|Canada[-\u2010]U\.S\.|
    Canadian[-\u2010]U\.S\.|Capt\.|Cia\.|Cie\.|Co\.|Col\.|Colo\.|
    Conn\.|Corp\.|Cos\.|D[-\u2010]Mass\.|Dec\.|Del\.|Dept\.|Dr\.|
    Drs\.|Etc\.|Feb\.|Fla\.|Ft\.|Ga\.|Gen\.|Gov\.|Hon\.|INC\.|
    Ill\.|Inc\.|Ind\.|Jan\.|Japan[-\u2010]U\.S\.|Jr\.|Kan\.|
    Korean[-\u2010]U\.S\.|Ky\.|La\.|Lt\.|Ltd\.|Maj\.|Mass\.|Md\.|
    Messrs\.|Mfg\.|Mich\.|Minn\.|Miss\.|Mo\.|Mr\.|Mrs\.|Ms\.|Neb\.
    |Nev\.|No\.|Nos\.|Nov\.|Oct\.|Okla\.|Ont\.|Ore\.|Pa\.|Ph\.|
    Prof\.|Prop\.|Pty\.|Rep\.|Reps\.|Rev\.|S\.p\.A\.|Sen\.|Sens\.|
    Sept\.|Sgt\.|Sino[-\u2010]U\.S\.|Sr\.|St\.|Ste\.|Tenn\.|Tex\.|
    U\.S\.[-\u2010]U\.K\.|U\.S\.[-\u2010]U\.S\.S\.R\.|Va\.|Vt\.|W\.Va\.|
    Wash\.|Wis\.|Wyo\.|a\.k\.a\.|a\.m\.|anti[-\u2010]U\.S\.|cap\.|
    etc\.|ft\.|i\.e\.|non[-\u2010]U\.S\.|office/dept\.|p\.m\.|
    president[-\u2010]U\.S\.|s\.r\.l\.|v\.|v\.B\.|v\.w\.|vs\.
)
""".lower(), re.UNICODE | re.VERBOSE)

class FrenchData(LanguageSpecificData):
    def __init__(self):
        self.clictics = re.compile(ur"""
            (?:
                # left clictics
                (?<!\w)     # should not be preceded by a letter
                (?:
                    [dcjlmnstDCJLNMST] | [Qq]u | [Jj]usqu | [Ll]orsqu
                )
                ['\u2019]   # apostrophe
                (?=\w)      # should be followed by a letter
                |
                # right clictics
                (?<=\w)     # should be preceded by a letter
                [-\u2010]   # hypen
                (?:
                    # FIXME!
                    [-\u2010]t[-\u2010]elles? | [-\u2010]t[-\u2010]ils? |
                    [-\u2010]t[-\u2010]on | [-\u2010]ce | [-\u2010]elles? |
                    [-\u2010]ils? | [-\u2010]je | [-\u2010]la | [-\u2010]les? |
                    [-\u2010]leur | [-\u2010]lui | [-\u2010]m\u00eames? |
                    [-\u2010]m['\u2019] | [-\u2010]moi | [-\u2010]nous | 
                    [-\u2010]on | [-\u2010]toi | [-\u2010]tu |
                    [-\u2010]t['\u2019] | [-\u2010]vous | [-\u2010]en |
                    [-\u2010]y | [-\u2010]ci | [-\u2010]l\u00e0
                )
                (?!w)      # should not be followed by a letter
            )
            """, re.UNICODE | re.VERBOSE)

        self.abbreviations = re.compile(ur"""
(?<!\w)     # should not be preceded by a letter
(?:
    rendez[-\u2010]vous|d['\u2019]abord|d['\u2019]accord|d['\u2019]ailleurs|
    d['\u2019]apr\u00e8s|d['\u2019]autant|d['\u2019]\u0153uvre|
    d['\u2019]oeuvre|c['\u2019]est[-\u2010]\u00e0[-\u2010]dire|
    moi[-\u2010]m\u00eame|toi[-\u2010]m\u00eame|lui[-\u2010]m\u00eame|
    elle[-\u2010]m\u00eame|nous[-\u2010]m\u00eames|vous[-\u2010]m\u00eames|
    eux[-\u2010]m\u00eames|elles[-\u2010]m\u00eames|par[-\u2010]ci|
    par[-\u2010]l\u00e0|Rendez[-\u2010]vous|D['\u2019]abord|D['\u2019]accord|
    D['\u2019]ailleurs|D['\u2019]apr\u00e8s|D['\u2019]autant|
    D['\u2019]\u0153uvre|D['\u2019]oeuvre|
    C['\u2019]est[-\u2010]\u00e0[-\u2010]dire|Moi[-\u2010]m\u00eame|
    Toi[-\u2010]m\u00eame|Lui[-\u2010]m\u00eame|Elle[-\u2010]m\u00eame|
    Nous[-\u2010]m\u00eames|Vous[-\u2010]m\u00eames|Eux[-\u2010]m\u00eames|
    Elles[-\u2010]m\u00eames|Par[-\u2010]ci|Par[-\u2010]l\u00e0
)
(?!w)      # should not be followed by a letter
""", re.UNICODE | re.VERBOSE)

class ItalianData(LanguageSpecificData):
    def __init__(self):
        self.clictics = re.compile(ur"""
            (?:
                # left clictics
                (?<!\w)     # should not be preceded by a letter
                (?:
                    [dD][ae]ll | [nN]ell | [Aa]ll | [lLDd] | [Ss]ull | [Qq]uest |
                    [Uu]n | [Ss]enz | [Tt]utt
                )
                ['\u2019]   # apostrophe
                (?=\w)      # should be followed by a letter
            )
            """, re.UNICODE | re.VERBOSE)

        self.abbreviations = re.compile(ur"""
            (?<!\w)     # should not be preceded by a letter
            (?:
                L\. | Lit\. | art\. | lett\. | n\. | no\. | pagg\. | prot\. | tel\.
            )
            """, re.UNICODE | re.VERBOSE)

class GermanData(LanguageSpecificData):
    def __init__(self):
        self.abbreviations = re.compile(ur"""
(?:
    # these can be preceded by a letter
    (?:
        [-\u2010]hdg\.|[-\u2010]tlg\.
    )
    |
    # these should not be preceded by a letter
    (?<!\w)
    (?:
        # from http://en.wiktionary.org/wiki/Category:German_abbreviations
        AB[-\u2010]Whg\.|Abl\.|Bio\.|Bj\.|Blk\.|Eigent\.[-\u2010]Whg\.|
        Eigent\.[-\u2010]Whgn\.|Eigt\.[-\u2010]Whg\.|Eigt\.[-\u2010]Whgn\.|Fr\.|
        Gal\.|Gart\.ant\.|Grd\.|Grdst\.|Hdt\.|Jg\.|Kl\.[-\u2010]Whg\.|
        Kl\.[-\u2010]Whgn\.|Mais\.[-\u2010]Whg\.|Mais\.[-\u2010]Whgn\.|Mio\.|
        Mrd\.|NB[-\u2010]Whg\.|Nb\.[-\u2010]Whg\.|Nb\.[-\u2010]Whgn\.|Nfl\.|
        Pak\.|Prov\.|Sout\.|Tsd\.|Whg\.|Whgn\.|Zi\.|Ziegelbauw\.|
        Ztr\.[-\u2010]Hzg\.|Ztrhzg\.|Zw\.[-\u2010]Whg\.|Zw\.[-\u2010]Whgn\.|
        abzgl\.|bezugsf\.|bzgl\.|bzw\.|d\.[ ]h\.|engl\.|freist\.|frz\.|
        i\.[ ]d\.[ ]R\.|m\u00f6bl\.|ren\.|ren\.bed\.|rest\.|san\.|usw\.|
        z\.[ ]B\.|zz\.|zzgl\.|zzt\.
    )
)
""", re.UNICODE | re.VERBOSE)

class DutchData(LanguageSpecificData):
    def __init__(self):
        self.abbreviations = re.compile(ur"""
(?:
    # these can be preceded by a letter
    (?:
        ['\u2019]t | ['\u2019]s | ['\u2019]n
    )
    |
    # these should not be preceded by a letter
    (?<!\w)
    (?:
        2bis\.|3bis\.|7bis\.|AR\.|Actualit\.|Afd\.|Antw\.|Arbh\.|Art\.|
        B\.St\.|B\.s\.|Besl\.W\.|Bull\.|Bull\.Bel\.|Cass\.|Cf\.|
        Com\.I\.B\.|D\.t/V\.I\.|Dhr\.|Doc\.|Dr\.|Fisc\.|Fr\.|Gec\.|II\.
        |III\.|J\.[-\u2010]L\.M\.|NR\.|NRS\.|Nat\.|No\.|Nr\.|Onderafd\.|
        PAR\.|Par\.|RECHTSFAK\.|RKW\.|TELEF\.|Volksvert\.|Vr\.|a\.|
        adv\.[-\u2010]gen\.|afd\.|aj\.|al\.|arb\.|art\.|artt\.|b\.|
        b\.v\.|b\.w\.|bijv\.|blz\.|bv\.|c\.q\.|cf\.|cfr\.|concl\.|d\.
        |d\.d\.|d\.i\.|d\.w\.z\.|dd\.|doc\.|e\.|e\.d\.|e\.v\.|enz\.|
        f\.|fr\.|g\.w\.|gepubl\.|i\.p\.v\.|i\.v\.m\.|j\.t\.t\.|jl\.|
        k\.b\.|kol\.|m\.b\.t\.|m\.i\.|max\.|n\.a\.v\.|nl\.|nr\.|nrs\.|
        o\.a\.|o\.b\.s\.i\.|o\.m\.|opm\.|p\.|par\.|pct\.|pp\.|ref\.|
        resp\.|respekt\.|t\.a\.v\.|t\.o\.v\.|vb\.|w\.
    )
)
""", re.UNICODE | re.VERBOSE)

class SpanishData(LanguageSpecificData):
    def __init__(self):
        self.abbreviations = re.compile(ur"""
            (?<!\w)     # should not be preceded by a letter
            (?:
                Ref\. | Vol\. | etc\. | App\. | Rec\.
            )
            """, re.UNICODE | re.VERBOSE)

### MAIN #####################################################################
LANGUAGE_DATA = {
    'english': EnglishData,
    'french' : FrenchData,
    'german' : GermanData,
    'italian': ItalianData,
    'spanish': SpanishData,
    'dutch'  : DutchData,
    'other'  : LanguageSpecificData,
}

def usage():
    return """Usage: unitok.py [OPTIONS] [FILES...]
Tokenize the FILES or standard input.

  -l, --language=LANG   language of the input
                        LANG is 'english', 'french', 'german', 'italian',
                                'spanish', 'dutch', or 'other'
                                (defaults to 'english')
  -e, --encoding=ENC    character encoding of the input
                        ENC is one of Codec or Alias values at
                        http://docs.python.org/library/codecs.html#id3
                        (defaults to 'utf-8')
  -n, --no-glue         do not add glue (<g/>) tags
  -s, --stream          process input line by line
                        WARNING: splits SGML tags if on multiple lines
  -q, --quiet           no warnings
  -h, --help
  
Description:
- splits input text into tokens (one token per line)
- for specified languages recognizes abbreviations and clictics (such as 've
  or n't in English)
- preserves SGML markup
- replaces SGML entities with unicode equivalents
- recognizes URLs, e-mail addreses, DNS domains, IP addresses
- adds glue (<g/>) tags between tokens not separated by space
- the output can be tagged with the TreeTagger part-of-speech tagger"""

def main(*args):
    import codecs
    import getopt
    import sys

    try:
        opts, args = getopt.getopt(args, "l:e:nsh",
                ["language=", "encoding=", "no-glue", "stream", "help"])
    except getopt.GetoptError, err:
        print >>sys.stderr, err
        print >>sys.stderr, usage()
        sys.exit(2)

    language = 'english'
    encoding = 'utf-8'
    no_glue  = False
    stream   = False
    quiet    = False
    for o, a in opts:
        if o in ("-l", "--language"):
            language = a.lower()
            if not LANGUAGE_DATA.has_key(language):
                print >>sys.stderr, "unsupported language: %s" % a
                print >>sys.stderr, "supported languages: %s" % ", ".join(LANGUAGE_DATA.keys())
                print >>sys.stderr
                print >>sys.stderr, usage()
                sys.exit(2)
        if o in ("-e", "--encoding"):
            encoding = a
            try:
                codecs.lookup(encoding)
            except:
                print >>sys.stderr, "unknown encoding: %s" % encoding
                print >>sys.stderr
                print >>sys.stderr, usage()
                sys.exit(2)
        if o in ("-n", "--no-glue"):
            no_glue = True
        if o in ("-s", "--stream"):
            stream = True
        if o in ("-q", "--quiet"):
            stream = True
        if o in ("-h", "--help"):
            print usage()
            sys.exit(0)

    lsd = LANGUAGE_DATA[language]()
    if no_glue:
        glue = None
    else:
        glue = GLUE_TAG

    input_files = args
    if not input_files:
        input_files = ['-'] # if no input files, use stdin

    for input_file in input_files:
        if input_file == '-':
            fp = sys.stdin
            fp_desc = 'stdin'
        else:
            fp = open(input_file, 'r')
            fp_desc = input_file
        try:
            if stream:
                first_line = True
                for (lineno0, line) in enumerate(fp):
                    try:
                        uline = unicode(line, encoding)
                    except UnicodeDecodeError, detail:
                        if not quiet:
                            print >>sys.stderr, "warning: %s, line %i: %s" % (
                                    fp_desc, lineno0+1, str(detail))
                        uline = unicode(line, encoding, 'replace')
                    tokens = tokenise(uline, lsd, glue)
                    if not first_line:
                        tokens.insert(0, u"") # force starting newline
                    sys.stdout.write(u"\n".join(tokens).encode(encoding, 'replace'))
                    first_line = False
            else:
                data = fp.read()
                try:
                    udata = unicode(data, encoding)
                except UnicodeDecodeError, detail:
                    if not quiet:
                        print >>sys.stderr, "warning: %s: %s" % (fp_desc, str(detail))
                    udata = unicode(data, encoding, 'replace')
                tokens = tokenise(udata, lsd, glue)
                sys.stdout.write(u"\n".join(tokens).encode(encoding, 'replace'))
        finally:
            if input_file != '-':
                fp.close()
            sys.stdout.write(u"\n".encode(encoding))

if __name__ == "__main__":
    import sys
    main(*(sys.argv[1:]))
