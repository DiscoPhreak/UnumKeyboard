#!/usr/bin/env python3
"""
Generate en-US dictionary data for Unum Keyboard.
Outputs unigrams.txt, bigrams.txt, trigrams.txt with realistic frequency distributions.
Targets: ~10,000 unigrams, ~5,000 bigrams, ~2,000 trigrams.
"""

import os
import math
import random

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "packs", "en-US")

random.seed(42)  # Reproducible output

# ============================================================
# UNIGRAMS
# ============================================================

# Top function words (freq 30000-100000)
TOP_WORDS = [
    ("the", 100000), ("of", 80000), ("and", 70000), ("to", 68000),
    ("a", 65000), ("in", 62000), ("is", 55000), ("that", 50000),
    ("it", 48000), ("for", 45000), ("was", 43000), ("on", 40000),
    ("are", 38000), ("be", 36000), ("with", 35000), ("as", 34000),
    ("i", 33000), ("have", 32000), ("at", 31000), ("this", 30000),
]

# Common words (freq 10000-29999)
COMMON_WORDS = [
    ("not", 29000), ("but", 28500), ("from", 28000), ("or", 27500),
    ("by", 27000), ("an", 26500), ("they", 26000), ("which", 25500),
    ("you", 25000), ("do", 24500), ("he", 24000), ("she", 23500),
    ("we", 23000), ("all", 22500), ("their", 22000), ("will", 21500),
    ("would", 21000), ("there", 20500), ("what", 20000), ("so", 19500),
    ("if", 19000), ("about", 18500), ("who", 18000), ("get", 17500),
    ("no", 17000), ("can", 16500), ("had", 16000), ("my", 15500),
    ("one", 15000), ("our", 14500), ("out", 14000), ("up", 13500),
    ("said", 13000), ("has", 12500), ("its", 12000), ("been", 11500),
    ("him", 11000), ("her", 10500), ("how", 10000),
]

# High frequency content words (freq 5000-9999)
HIGH_FREQ = [
    ("when", 9900), ("more", 9800), ("very", 9700), ("some", 9600),
    ("time", 9500), ("could", 9400), ("than", 9300), ("other", 9200),
    ("into", 9100), ("then", 9000), ("now", 8900), ("only", 8800),
    ("just", 8700), ("also", 8600), ("know", 8500), ("like", 8400),
    ("way", 8300), ("may", 8200), ("well", 8100), ("because", 8000),
    ("these", 7900), ("two", 7800), ("each", 7700), ("make", 7600),
    ("first", 7500), ("people", 7400), ("new", 7300), ("over", 7200),
    ("after", 7100), ("should", 7000), ("any", 6900), ("most", 6800),
    ("where", 6700), ("between", 6600), ("through", 6500), ("back", 6400),
    ("much", 6300), ("before", 6200), ("good", 6100), ("think", 6000),
    ("long", 5900), ("right", 5800), ("still", 5700), ("own", 5600),
    ("too", 5500), ("same", 5400), ("tell", 5300), ("does", 5200),
    ("set", 5100), ("three", 5000),
]

# Medium frequency words (freq 2000-4999)
MED_FREQ_WORDS = [
    ("need", 4900), ("want", 4800), ("look", 4700), ("here", 4600),
    ("many", 4500), ("thing", 4400), ("see", 4300), ("take", 4200),
    ("day", 4100), ("come", 4000), ("work", 3900), ("made", 3800),
    ("go", 3700), ("find", 3600), ("call", 3500), ("again", 3400),
    ("even", 3300), ("give", 3200), ("old", 3100), ("every", 3000),
    ("year", 2900), ("great", 2800), ("last", 2700), ("big", 2600),
    ("say", 2500), ("while", 2400), ("world", 2300), ("keep", 2200),
    ("never", 2100), ("help", 2000),
]

# Large word lists by category for programmatic expansion
NOUNS = [
    "time", "year", "people", "way", "day", "man", "woman", "child", "world", "life",
    "hand", "part", "place", "case", "week", "company", "system", "program", "question", "work",
    "government", "number", "night", "point", "home", "water", "room", "mother", "area", "money",
    "story", "fact", "month", "lot", "right", "study", "book", "eye", "job", "word",
    "business", "issue", "side", "kind", "head", "house", "service", "friend", "father", "power",
    "hour", "game", "line", "end", "member", "law", "car", "city", "community", "name",
    "president", "team", "minute", "idea", "body", "information", "school", "family", "student", "group",
    "country", "problem", "change", "food", "face", "door", "health", "person", "art", "war",
    "history", "party", "result", "morning", "reason", "research", "girl", "guy", "moment", "air",
    "teacher", "force", "education", "table", "class", "office", "market", "plan", "product", "price",
    "paper", "age", "voice", "letter", "music", "field", "movie", "tv", "phone", "computer",
    "building", "road", "street", "window", "bed", "garden", "song", "baby", "dog", "cat",
    "color", "size", "shape", "form", "level", "stage", "nature", "culture", "season", "weather",
    "center", "church", "ground", "picture", "kitchen", "island", "river", "lake", "ocean", "mountain",
    "tree", "flower", "animal", "fish", "bird", "horse", "star", "sun", "moon", "earth",
    "fire", "light", "stone", "glass", "metal", "wood", "wall", "floor", "door", "corner",
    "doctor", "nurse", "teacher", "lawyer", "judge", "officer", "soldier", "driver", "manager", "leader",
    "king", "queen", "prince", "princess", "hero", "angel", "god", "spirit", "soul", "mind",
    "heart", "brain", "blood", "bone", "skin", "hair", "arm", "leg", "foot", "finger",
    "shoulder", "knee", "neck", "chest", "stomach", "muscle", "tooth", "lip", "tongue", "ear",
    "brother", "sister", "son", "daughter", "husband", "wife", "uncle", "aunt", "cousin", "grandfather",
    "grandmother", "parent", "neighbor", "boss", "colleague", "partner", "customer", "patient", "client", "guest",
    "audience", "crowd", "population", "society", "generation", "tradition", "religion", "faith", "prayer", "church",
    "truth", "lie", "secret", "mystery", "dream", "hope", "fear", "pain", "love", "hate",
    "anger", "joy", "peace", "war", "freedom", "justice", "truth", "beauty", "strength", "weakness",
    "knowledge", "skill", "talent", "ability", "effort", "success", "failure", "mistake", "lesson", "experience",
    "chance", "choice", "option", "decision", "opinion", "idea", "thought", "theory", "fact", "evidence",
    "news", "report", "article", "message", "email", "text", "post", "comment", "review", "response",
    "conversation", "discussion", "argument", "debate", "speech", "interview", "meeting", "event", "party", "wedding",
    "birthday", "holiday", "vacation", "trip", "journey", "adventure", "tour", "visit", "stay", "flight",
    "ticket", "hotel", "restaurant", "shop", "store", "mall", "bank", "hospital", "library", "museum",
    "station", "airport", "park", "beach", "farm", "forest", "desert", "jungle", "cave", "bridge",
    "tower", "castle", "palace", "temple", "stadium", "theater", "cinema", "gallery", "studio", "lab",
    "factory", "warehouse", "garage", "basement", "attic", "hallway", "bathroom", "bedroom", "balcony", "porch",
    "roof", "ceiling", "stair", "elevator", "entrance", "exit", "path", "trail", "highway", "tunnel",
    "border", "coast", "shore", "cliff", "valley", "hill", "plain", "volcano", "earthquake", "storm",
    "rain", "snow", "wind", "cloud", "sky", "rainbow", "thunder", "lightning", "fog", "ice",
    "wave", "current", "tide", "flood", "drought", "heat", "cold", "temperature", "climate", "atmosphere",
    "energy", "fuel", "oil", "gas", "coal", "electricity", "battery", "engine", "machine", "tool",
    "weapon", "bomb", "gun", "knife", "sword", "shield", "armor", "helmet", "uniform", "flag",
    "medal", "prize", "award", "trophy", "gift", "present", "surprise", "joke", "trick", "puzzle",
    "game", "sport", "match", "race", "team", "player", "coach", "referee", "score", "goal",
    "ball", "bat", "net", "court", "track", "pool", "ring", "gym", "club", "league",
    "champion", "winner", "loser", "record", "title", "season", "round", "series", "final", "playoff",
    "breakfast", "lunch", "dinner", "meal", "snack", "drink", "coffee", "tea", "juice", "milk",
    "bread", "butter", "cheese", "egg", "meat", "chicken", "fish", "rice", "pasta", "soup",
    "salad", "fruit", "apple", "banana", "orange", "grape", "strawberry", "lemon", "tomato", "potato",
    "onion", "pepper", "garlic", "sugar", "salt", "oil", "sauce", "cream", "chocolate", "cake",
    "cookie", "pie", "candy", "ice", "wine", "beer", "bottle", "cup", "plate", "bowl",
    "fork", "spoon", "knife", "pan", "pot", "oven", "stove", "fridge", "sink", "dish",
    "shirt", "pants", "dress", "jacket", "coat", "hat", "shoe", "boot", "sock", "glove",
    "belt", "tie", "scarf", "bag", "pocket", "button", "zipper", "collar", "sleeve", "fabric",
    "cotton", "silk", "leather", "gold", "silver", "diamond", "ring", "necklace", "bracelet", "watch",
    "clock", "calendar", "schedule", "alarm", "bell", "key", "lock", "chain", "rope", "wire",
    "string", "tape", "glue", "paint", "brush", "pen", "pencil", "paper", "notebook", "folder",
    "box", "bag", "basket", "container", "jar", "can", "bottle", "package", "envelope", "stamp",
    "coin", "bill", "cash", "credit", "debt", "tax", "fee", "profit", "loss", "budget",
    "salary", "wage", "income", "expense", "payment", "receipt", "invoice", "account", "balance", "loan",
    "insurance", "investment", "stock", "share", "bond", "fund", "asset", "property", "estate", "rent",
    "mortgage", "contract", "agreement", "deal", "offer", "bid", "auction", "sale", "discount", "coupon",
    "warranty", "guarantee", "return", "exchange", "refund", "delivery", "shipment", "order", "catalog", "brand",
    "model", "version", "design", "pattern", "style", "fashion", "trend", "feature", "quality", "standard",
    "software", "hardware", "network", "internet", "website", "app", "data", "file", "code", "program",
    "database", "server", "cloud", "platform", "device", "screen", "keyboard", "mouse", "camera", "speaker",
    "microphone", "headphone", "printer", "scanner", "monitor", "laptop", "tablet", "smartphone", "charger", "cable",
    "wifi", "bluetooth", "signal", "connection", "download", "upload", "update", "backup", "password", "username",
    "profile", "setting", "notification", "message", "chat", "video", "photo", "image", "audio", "stream",
    "channel", "content", "media", "blog", "podcast", "forum", "community", "follower", "subscriber", "viewer",
    "algorithm", "search", "filter", "sort", "tag", "link", "page", "site", "domain", "browser",
    "traffic", "visitor", "click", "view", "share", "like", "comment", "post", "feed", "story",
    "advertisement", "campaign", "promotion", "sponsor", "brand", "logo", "slogan", "marketing", "advertising", "sales",
    "customer", "consumer", "buyer", "seller", "vendor", "supplier", "manufacturer", "producer", "distributor", "retailer",
    "doctor", "surgeon", "dentist", "therapist", "psychologist", "pharmacist", "scientist", "researcher", "engineer", "architect",
    "designer", "artist", "writer", "author", "journalist", "reporter", "editor", "publisher", "director", "producer",
    "actor", "singer", "musician", "dancer", "painter", "photographer", "filmmaker", "poet", "novelist", "playwright",
    "chef", "baker", "bartender", "waiter", "pilot", "captain", "sailor", "mechanic", "electrician", "plumber",
    "carpenter", "farmer", "gardener", "fisherman", "hunter", "miner", "guard", "detective", "spy", "thief",
    "criminal", "prisoner", "victim", "witness", "suspect", "defendant", "plaintiff", "attorney", "prosecutor", "jury",
    "trial", "hearing", "sentence", "verdict", "appeal", "bail", "parole", "prison", "jail", "cell",
    "crime", "murder", "theft", "robbery", "fraud", "assault", "kidnapping", "arson", "vandalism", "bribery",
    "corruption", "conspiracy", "terrorism", "violence", "abuse", "harassment", "discrimination", "racism", "sexism", "poverty",
    "hunger", "disease", "epidemic", "pandemic", "infection", "virus", "bacteria", "symptom", "diagnosis", "treatment",
    "medicine", "drug", "pill", "injection", "surgery", "therapy", "recovery", "cure", "vaccine", "dose",
    "hospital", "clinic", "pharmacy", "ambulance", "emergency", "patient", "nurse", "doctor", "surgeon", "specialist",
    "science", "biology", "chemistry", "physics", "math", "algebra", "geometry", "calculus", "statistics", "probability",
    "experiment", "hypothesis", "observation", "measurement", "analysis", "conclusion", "formula", "equation", "variable", "constant",
    "atom", "molecule", "cell", "gene", "dna", "protein", "enzyme", "bacteria", "virus", "organism",
    "species", "evolution", "habitat", "ecosystem", "environment", "pollution", "conservation", "sustainability", "recycling", "emission",
    "carbon", "oxygen", "hydrogen", "nitrogen", "iron", "copper", "aluminum", "plastic", "rubber", "concrete",
    "steel", "ceramic", "fiber", "crystal", "mineral", "rock", "sand", "soil", "mud", "dust",
    "planet", "galaxy", "universe", "satellite", "rocket", "astronaut", "orbit", "gravity", "radiation", "telescope",
    "language", "grammar", "vocabulary", "pronunciation", "accent", "dialect", "translation", "interpretation", "alphabet", "syllable",
    "sentence", "paragraph", "chapter", "novel", "essay", "poem", "lyric", "verse", "rhyme", "metaphor",
    "symbol", "sign", "gesture", "expression", "emotion", "feeling", "mood", "attitude", "personality", "character",
    "behavior", "habit", "routine", "ritual", "ceremony", "custom", "tradition", "legend", "myth", "tale",
    "fiction", "fantasy", "horror", "mystery", "romance", "comedy", "drama", "tragedy", "thriller", "adventure",
    "war", "battle", "fight", "conflict", "struggle", "revolution", "rebellion", "protest", "riot", "strike",
    "election", "vote", "ballot", "candidate", "campaign", "debate", "policy", "reform", "amendment", "constitution",
    "democracy", "republic", "monarchy", "dictatorship", "empire", "colony", "territory", "province", "state", "county",
    "district", "region", "zone", "border", "boundary", "frontier", "capital", "suburb", "village", "town",
    "map", "compass", "direction", "distance", "location", "position", "coordinate", "latitude", "longitude", "altitude",
    "north", "south", "east", "west", "left", "right", "center", "middle", "top", "bottom",
    "front", "back", "inside", "outside", "above", "below", "surface", "edge", "corner", "side",
    "width", "height", "depth", "length", "weight", "volume", "area", "space", "gap", "hole",
    "circle", "square", "triangle", "rectangle", "sphere", "cube", "cylinder", "cone", "pyramid", "angle",
    "curve", "line", "dot", "point", "mark", "spot", "patch", "stripe", "pattern", "texture",
    "color", "shade", "tone", "hue", "brightness", "contrast", "shadow", "reflection", "mirror", "lens",
    "frame", "border", "margin", "padding", "spacing", "alignment", "layout", "grid", "column", "row",
    "chart", "graph", "diagram", "table", "list", "menu", "option", "button", "icon", "label",
    "title", "heading", "subtitle", "caption", "description", "summary", "overview", "introduction", "conclusion", "appendix",
    "index", "glossary", "reference", "footnote", "citation", "source", "bibliography", "archive", "collection", "catalog",
    "section", "chapter", "unit", "module", "component", "element", "item", "piece", "fragment", "sample",
    "copy", "original", "duplicate", "version", "edition", "draft", "revision", "update", "upgrade", "patch",
    "error", "bug", "crash", "issue", "problem", "solution", "fix", "workaround", "hack", "trick",
    "tip", "hint", "clue", "guide", "tutorial", "manual", "handbook", "instruction", "direction", "recipe",
    "step", "stage", "phase", "round", "cycle", "loop", "sequence", "series", "chain", "queue",
    "stack", "heap", "tree", "branch", "leaf", "root", "node", "edge", "path", "route",
    "shortcut", "detour", "bypass", "alternative", "substitute", "replacement", "equivalent", "match", "pair", "set",
    "group", "batch", "cluster", "bundle", "pack", "kit", "suite", "combo", "mix", "blend",
]

VERBS = [
    "be", "have", "do", "say", "get", "make", "go", "know", "take", "see",
    "come", "think", "look", "want", "give", "use", "find", "tell", "ask", "work",
    "seem", "feel", "try", "leave", "call", "need", "become", "keep", "let", "begin",
    "show", "hear", "play", "run", "move", "live", "believe", "bring", "happen", "write",
    "provide", "sit", "stand", "lose", "pay", "meet", "include", "continue", "learn", "change",
    "lead", "understand", "watch", "follow", "stop", "create", "speak", "read", "allow", "add",
    "spend", "grow", "open", "walk", "win", "offer", "remember", "love", "consider", "appear",
    "buy", "wait", "serve", "die", "send", "expect", "build", "stay", "fall", "cut",
    "reach", "kill", "remain", "suggest", "raise", "pass", "sell", "require", "report", "decide",
    "pull", "develop", "eat", "break", "pick", "push", "drop", "drive", "join", "catch",
    "draw", "choose", "wear", "fight", "throw", "sing", "teach", "sleep", "hang", "rise",
    "fill", "carry", "hide", "share", "hold", "shut", "cry", "laugh", "smile", "dance",
    "cook", "clean", "wash", "fix", "check", "test", "count", "measure", "compare", "match",
    "search", "sort", "save", "load", "delete", "copy", "paste", "print", "scan", "type",
    "click", "scroll", "swipe", "tap", "drag", "zoom", "refresh", "restart", "install", "update",
    "download", "upload", "stream", "record", "edit", "format", "convert", "export", "import", "sync",
    "connect", "disconnect", "log", "register", "subscribe", "unsubscribe", "follow", "block", "report", "flag",
    "approve", "reject", "accept", "decline", "confirm", "cancel", "submit", "apply", "request", "invite",
    "remind", "notify", "alert", "warn", "inform", "advise", "recommend", "suggest", "propose", "negotiate",
    "agree", "disagree", "argue", "debate", "discuss", "explain", "describe", "define", "clarify", "demonstrate",
    "illustrate", "represent", "indicate", "imply", "mean", "express", "communicate", "translate", "interpret", "pronounce",
    "spell", "guess", "predict", "estimate", "calculate", "solve", "prove", "discover", "invent", "design",
    "plan", "organize", "arrange", "prepare", "practice", "perform", "produce", "manufacture", "assemble", "construct",
    "repair", "maintain", "replace", "remove", "install", "attach", "separate", "combine", "mix", "blend",
    "heat", "cool", "freeze", "melt", "boil", "bake", "fry", "grill", "roast", "steam",
    "chop", "slice", "peel", "squeeze", "pour", "stir", "spread", "wrap", "fold", "roll",
    "paint", "sketch", "carve", "sculpt", "weave", "knit", "sew", "iron", "polish", "shine",
    "climb", "crawl", "jump", "hop", "skip", "slide", "spin", "twist", "bend", "stretch",
    "lift", "lower", "raise", "shake", "wave", "point", "grab", "squeeze", "touch", "press",
    "rub", "scratch", "pat", "stroke", "hug", "kiss", "bite", "kick", "punch", "slap",
    "breathe", "cough", "sneeze", "yawn", "sigh", "whisper", "shout", "scream", "whistle", "clap",
    "knock", "ring", "buzz", "beep", "crash", "bang", "click", "snap", "pop", "crack",
    "bloom", "grow", "spread", "shrink", "expand", "increase", "decrease", "double", "triple", "multiply",
    "divide", "subtract", "add", "total", "average", "balance", "adjust", "modify", "transform", "convert",
    "earn", "invest", "borrow", "lend", "owe", "charge", "refund", "donate", "fund", "sponsor",
    "elect", "appoint", "promote", "retire", "resign", "hire", "fire", "train", "mentor", "coach",
    "motivate", "inspire", "encourage", "support", "assist", "guide", "direct", "manage", "supervise", "coordinate",
    "celebrate", "honor", "respect", "admire", "appreciate", "thank", "praise", "compliment", "greet", "welcome",
    "forgive", "apologize", "excuse", "pardon", "blame", "accuse", "deny", "confess", "admit", "regret",
    "worry", "stress", "panic", "relax", "calm", "meditate", "focus", "concentrate", "distract", "bore",
    "amuse", "entertain", "impress", "surprise", "shock", "scare", "frighten", "terrify", "annoy", "irritate",
    "frustrate", "disappoint", "satisfy", "please", "delight", "excite", "thrill", "overwhelm", "confuse", "puzzle",
]

ADJECTIVES = [
    "good", "new", "first", "last", "long", "great", "little", "own", "other", "old",
    "right", "big", "high", "different", "small", "large", "next", "early", "young", "important",
    "few", "public", "bad", "same", "able", "free", "sure", "real", "full", "special",
    "easy", "clear", "recent", "certain", "personal", "open", "red", "hard", "simple", "possible",
    "whole", "strong", "true", "fast", "short", "single", "dark", "low", "general", "specific",
    "close", "final", "main", "happy", "serious", "ready", "left", "physical", "social", "natural",
    "local", "common", "current", "likely", "nice", "hot", "cold", "warm", "cool", "fresh",
    "clean", "dirty", "dry", "wet", "soft", "loud", "quiet", "sharp", "smooth", "rough",
    "thick", "thin", "wide", "narrow", "deep", "shallow", "flat", "round", "straight", "tight",
    "loose", "empty", "heavy", "light", "bright", "pale", "rich", "poor", "cheap", "expensive",
    "safe", "dangerous", "healthy", "sick", "tired", "awake", "alive", "dead", "hungry", "thirsty",
    "busy", "calm", "angry", "sad", "afraid", "nervous", "proud", "guilty", "lonely", "bored",
    "curious", "confident", "creative", "intelligent", "wise", "brave", "gentle", "kind", "cruel", "honest",
    "fair", "loyal", "patient", "generous", "selfish", "lazy", "active", "popular", "famous", "powerful",
    "useful", "beautiful", "ugly", "pretty", "cute", "funny", "strange", "weird", "crazy", "normal",
    "perfect", "terrible", "wonderful", "amazing", "awesome", "fantastic", "excellent", "brilliant", "gorgeous", "stunning",
    "sweet", "bitter", "sour", "salty", "spicy", "delicious", "tasty", "yummy", "gross", "plain",
    "basic", "advanced", "complex", "modern", "ancient", "classic", "traditional", "original", "unique", "rare",
    "common", "typical", "average", "ordinary", "extreme", "absolute", "complete", "total", "entire", "partial",
    "separate", "individual", "independent", "mutual", "equal", "similar", "opposite", "alternative", "additional", "extra",
    "primary", "secondary", "minor", "major", "critical", "essential", "necessary", "optional", "available", "accessible",
    "visible", "obvious", "apparent", "subtle", "slight", "mild", "moderate", "severe", "intense", "extreme",
    "positive", "negative", "neutral", "stable", "steady", "constant", "variable", "random", "regular", "frequent",
    "occasional", "rare", "temporary", "permanent", "annual", "daily", "weekly", "monthly", "immediate", "sudden",
    "gradual", "rapid", "slow", "quick", "instant", "brief", "lengthy", "endless", "infinite", "tiny",
    "huge", "massive", "enormous", "giant", "miniature", "compact", "portable", "mobile", "remote", "virtual",
    "digital", "electronic", "automatic", "manual", "professional", "amateur", "official", "formal", "informal", "casual",
]

ADVERBS = [
    "not", "also", "very", "often", "however", "too", "usually", "really", "already", "always",
    "sometimes", "never", "here", "there", "now", "then", "today", "tomorrow", "yesterday", "again",
    "just", "still", "even", "quite", "almost", "enough", "well", "probably", "maybe", "perhaps",
    "certainly", "definitely", "absolutely", "completely", "totally", "entirely", "exactly", "simply", "merely", "basically",
    "actually", "really", "truly", "honestly", "seriously", "literally", "practically", "essentially", "mainly", "mostly",
    "partly", "slightly", "somewhat", "fairly", "rather", "pretty", "extremely", "incredibly", "amazingly", "surprisingly",
    "suddenly", "quickly", "slowly", "carefully", "easily", "hardly", "recently", "finally", "eventually", "immediately",
    "currently", "previously", "originally", "recently", "frequently", "rarely", "regularly", "constantly", "gradually", "rapidly",
    "directly", "automatically", "manually", "publicly", "privately", "personally", "professionally", "officially", "formally", "casually",
    "perfectly", "properly", "correctly", "accurately", "roughly", "approximately", "exactly", "precisely", "clearly", "obviously",
]

PREPOSITIONS = [
    "about", "above", "across", "after", "against", "along", "among", "around", "at", "before",
    "behind", "below", "beneath", "beside", "between", "beyond", "by", "despite", "down", "during",
    "except", "for", "from", "in", "inside", "into", "near", "of", "off", "on",
    "onto", "out", "outside", "over", "past", "since", "through", "throughout", "to", "toward",
    "under", "until", "up", "upon", "with", "within", "without",
]

CONJUNCTIONS = [
    "and", "but", "or", "nor", "for", "yet", "so", "because", "although", "though",
    "while", "whereas", "unless", "until", "since", "whether", "if", "when", "where", "after",
    "before", "once", "whenever", "wherever", "however", "therefore", "moreover", "furthermore", "nevertheless", "meanwhile",
]

PRONOUNS = [
    "i", "me", "my", "mine", "myself",
    "you", "your", "yours", "yourself",
    "he", "him", "his", "himself",
    "she", "her", "hers", "herself",
    "it", "its", "itself",
    "we", "us", "our", "ours", "ourselves",
    "they", "them", "their", "theirs", "themselves",
    "who", "whom", "whose", "which", "that",
    "this", "these", "those", "what", "whoever",
    "whatever", "each", "every", "either", "neither",
    "both", "all", "any", "few", "many",
    "some", "several", "enough", "nobody", "nothing",
    "everyone", "everything", "someone", "something", "anyone",
    "anything", "somebody", "anybody", "nowhere", "everywhere",
]

NUMBERS_AND_ORDINALS = [
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
    "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
    "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety", "hundred", "thousand",
    "million", "billion", "trillion", "first", "second", "third", "fourth", "fifth", "sixth", "seventh",
    "eighth", "ninth", "tenth", "once", "twice", "double", "triple", "half", "quarter", "dozen",
]

TECH_AND_INTERNET = [
    "email", "password", "login", "logout", "signup", "username", "account", "profile", "settings", "notification",
    "wifi", "bluetooth", "internet", "website", "browser", "google", "facebook", "twitter", "instagram", "youtube",
    "tiktok", "snapchat", "reddit", "linkedin", "pinterest", "whatsapp", "telegram", "discord", "zoom", "skype",
    "app", "download", "upload", "install", "update", "software", "hardware", "device", "phone", "laptop",
    "tablet", "desktop", "monitor", "keyboard", "mouse", "speaker", "headphones", "microphone", "camera", "printer",
    "server", "cloud", "database", "network", "router", "modem", "firewall", "vpn", "proxy", "encryption",
    "algorithm", "api", "html", "css", "javascript", "python", "java", "android", "ios", "windows",
    "linux", "mac", "chrome", "firefox", "safari", "edge", "opera", "http", "url", "domain",
    "hosting", "ssl", "dns", "ip", "bandwidth", "latency", "cache", "cookie", "session", "token",
    "git", "github", "code", "debug", "compile", "deploy", "test", "build", "release", "version",
    "ai", "ml", "chatbot", "automation", "robotics", "blockchain", "crypto", "bitcoin", "ethereum", "nft",
    "vr", "ar", "gaming", "esports", "streaming", "podcast", "blog", "vlog", "meme", "viral",
    "hashtag", "trending", "influencer", "follower", "subscriber", "content", "creator", "platform", "startup", "tech",
    "pixel", "resolution", "hd", "uhd", "fps", "gpu", "cpu", "ram", "ssd", "usb",
    "hdmi", "led", "oled", "amoled", "touchscreen", "gesture", "swipe", "pinch", "tap", "scroll",
]

CASUAL_AND_TEXTING = [
    "ok", "okay", "yeah", "yep", "nope", "nah", "yay", "wow", "cool", "nice",
    "awesome", "amazing", "great", "sweet", "sick", "dope", "lit", "fire", "epic", "legendary",
    "lol", "lmao", "rofl", "haha", "hehe", "omg", "wtf", "smh", "brb", "btw",
    "imo", "imho", "fyi", "idk", "tbh", "irl", "dm", "pm", "asap", "eta",
    "gonna", "wanna", "gotta", "kinda", "sorta", "lemme", "gimme", "dunno", "ain't", "y'all",
    "hey", "hi", "hello", "bye", "goodbye", "thanks", "thx", "please", "pls", "sorry",
    "sup", "yo", "dude", "bro", "sis", "fam", "babe", "hun", "bestie", "squad",
    "selfie", "emoji", "gif", "sticker", "avatar", "username", "handle", "bio", "feed", "story",
    "vibe", "mood", "flex", "slay", "stan", "ship", "cringe", "based", "mid", "cap",
    "sus", "salty", "savage", "goat", "clutch", "lowkey", "highkey", "deadass", "periodt", "bet",
]

DAYS_MONTHS_TIME = [
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
    "january", "february", "march", "april", "may", "june", "july", "august",
    "september", "october", "november", "december",
    "morning", "afternoon", "evening", "night", "midnight", "noon", "dawn", "dusk", "sunset", "sunrise",
    "second", "minute", "hour", "day", "week", "month", "year", "decade", "century", "millennium",
    "spring", "summer", "autumn", "fall", "winter",
    "today", "tomorrow", "yesterday", "tonight", "weekend", "weekday", "daily", "weekly", "monthly", "yearly",
]

COUNTRIES_AND_PLACES = [
    "america", "canada", "mexico", "brazil", "argentina", "chile", "colombia", "peru",
    "england", "france", "germany", "spain", "italy", "portugal", "netherlands", "belgium",
    "russia", "china", "japan", "korea", "india", "australia", "zealand",
    "africa", "egypt", "nigeria", "kenya", "morocco", "ethiopia",
    "turkey", "iran", "iraq", "israel", "saudi", "dubai", "pakistan", "afghanistan",
    "london", "paris", "berlin", "rome", "madrid", "tokyo", "beijing", "delhi",
    "york", "angeles", "francisco", "chicago", "houston", "seattle", "boston", "miami", "denver", "atlanta",
    "toronto", "vancouver", "sydney", "melbourne", "singapore", "bangkok", "jakarta",
    "europe", "asia", "pacific", "atlantic", "arctic", "antarctic", "caribbean", "mediterranean",
    "american", "british", "french", "german", "spanish", "italian", "chinese", "japanese", "indian", "russian",
    "european", "african", "asian", "australian", "canadian", "mexican", "brazilian", "korean",
]

EMOTIONS_AND_EXPRESSIONS = [
    "love", "hate", "like", "dislike", "enjoy", "prefer", "adore", "despise",
    "happy", "sad", "angry", "scared", "excited", "nervous", "anxious", "worried",
    "grateful", "thankful", "sorry", "apologize", "forgive", "blame", "regret", "wish",
    "hope", "dream", "fear", "doubt", "trust", "believe", "wonder", "imagine",
    "miss", "remember", "forget", "realize", "notice", "recognize", "understand", "confuse",
    "agree", "disagree", "accept", "refuse", "deny", "admit", "insist", "demand",
    "promise", "swear", "guarantee", "warn", "threaten", "challenge", "dare", "risk",
    "congratulations", "cheers", "welcome", "farewell", "bravo", "encore", "oops", "whoops",
    "ugh", "yikes", "geez", "gosh", "dang", "shoot", "darn", "phew", "whew", "ahem",
]

BUSINESS_AND_WORK = [
    "meeting", "conference", "presentation", "proposal", "report", "deadline", "schedule",
    "project", "task", "assignment", "milestone", "objective", "strategy", "budget",
    "revenue", "profit", "growth", "performance", "efficiency", "productivity", "innovation",
    "leadership", "teamwork", "collaboration", "communication", "negotiation", "management",
    "employee", "employer", "intern", "freelancer", "consultant", "contractor", "executive",
    "ceo", "cto", "cfo", "vp", "director", "manager", "supervisor", "coordinator",
    "resume", "interview", "qualification", "experience", "reference", "promotion", "retirement",
    "startup", "corporation", "enterprise", "organization", "institution", "agency", "department",
    "industry", "sector", "market", "competition", "monopoly", "merger", "acquisition", "partnership",
]

HEALTH_AND_WELLNESS = [
    "health", "wellness", "fitness", "exercise", "workout", "yoga", "meditation", "nutrition",
    "diet", "weight", "calories", "protein", "vitamin", "supplement", "organic", "vegan",
    "vegetarian", "gluten", "allergy", "intolerance", "headache", "fever", "cough", "cold",
    "flu", "covid", "vaccine", "mask", "quarantine", "isolation", "testing", "positive",
    "negative", "symptom", "treatment", "medication", "prescription", "pharmacy", "appointment",
    "checkup", "screening", "scan", "xray", "mri", "blood", "pressure", "sugar", "cholesterol",
    "mental", "anxiety", "depression", "stress", "burnout", "insomnia", "addiction", "recovery",
    "therapy", "counseling", "mindfulness", "breathing", "stretching", "cardio", "strength", "flexibility",
]

EDUCATION = [
    "school", "college", "university", "campus", "classroom", "lecture", "seminar", "workshop",
    "course", "class", "subject", "topic", "lesson", "homework", "assignment", "project",
    "exam", "test", "quiz", "grade", "score", "gpa", "degree", "diploma",
    "bachelor", "master", "doctorate", "phd", "professor", "instructor", "tutor", "mentor",
    "student", "freshman", "sophomore", "junior", "senior", "graduate", "alumni", "scholarship",
    "textbook", "curriculum", "syllabus", "semester", "quarter", "enrollment", "registration", "graduation",
    "research", "thesis", "dissertation", "publication", "journal", "conference", "symposium", "laboratory",
]

FOOD_EXTRA = [
    "sushi", "pizza", "burger", "sandwich", "taco", "burrito", "noodle", "curry",
    "steak", "bacon", "sausage", "ham", "turkey", "shrimp", "lobster", "crab",
    "avocado", "mushroom", "broccoli", "spinach", "cucumber", "carrot", "corn", "bean",
    "almond", "walnut", "peanut", "cashew", "coconut", "mango", "pineapple", "watermelon",
    "blueberry", "raspberry", "cherry", "peach", "pear", "plum", "fig", "olive",
    "yogurt", "cereal", "granola", "oatmeal", "pancake", "waffle", "toast", "bagel",
    "donut", "muffin", "croissant", "pretzel", "popcorn", "chip", "cracker", "biscuit",
    "smoothie", "latte", "espresso", "cappuccino", "mocha", "cocktail", "whiskey", "vodka",
]

ENTERTAINMENT = [
    "movie", "film", "show", "series", "episode", "season", "sequel", "prequel",
    "trailer", "premiere", "release", "soundtrack", "score", "plot", "character", "scene",
    "genre", "action", "comedy", "drama", "horror", "thriller", "romance", "animation",
    "documentary", "concert", "festival", "performance", "exhibition", "gallery", "museum",
    "book", "novel", "magazine", "newspaper", "comic", "manga", "anime", "cartoon",
    "music", "song", "album", "single", "playlist", "remix", "cover", "original",
    "rock", "pop", "jazz", "blues", "classical", "country", "rap", "hip",
    "guitar", "piano", "drum", "bass", "violin", "flute", "saxophone", "trumpet",
]

TRAVEL = [
    "travel", "trip", "journey", "vacation", "holiday", "adventure", "exploration", "tour",
    "flight", "airline", "airport", "terminal", "gate", "boarding", "passport", "visa",
    "luggage", "suitcase", "backpack", "reservation", "booking", "itinerary", "destination", "departure",
    "arrival", "delay", "cancellation", "connection", "layover", "transit", "customs", "immigration",
    "hotel", "hostel", "resort", "motel", "airbnb", "accommodation", "checkin", "checkout",
    "room", "suite", "lobby", "reception", "concierge", "amenity", "pool", "spa",
    "taxi", "uber", "lyft", "subway", "metro", "bus", "train", "ferry",
    "rental", "cruise", "highway", "freeway", "toll", "parking", "traffic", "navigation",
]

SPORTS = [
    "football", "soccer", "basketball", "baseball", "hockey", "tennis", "golf", "volleyball",
    "cricket", "rugby", "boxing", "wrestling", "karate", "judo", "fencing", "archery",
    "swimming", "diving", "surfing", "skiing", "snowboarding", "skating", "cycling", "running",
    "marathon", "sprint", "relay", "hurdle", "javelin", "discus", "shotput", "triathlon",
    "olympics", "championship", "tournament", "league", "cup", "medal", "trophy", "record",
    "athlete", "player", "team", "coach", "referee", "umpire", "spectator", "fan",
    "stadium", "arena", "field", "court", "track", "rink", "course", "pitch",
    "score", "point", "goal", "touchdown", "homerun", "slam", "ace", "birdie",
]

NATURE_AND_ANIMALS = [
    "nature", "wildlife", "forest", "jungle", "mountain", "valley", "river", "lake",
    "ocean", "sea", "beach", "island", "desert", "prairie", "meadow", "swamp",
    "tree", "flower", "grass", "bush", "vine", "moss", "fern", "cactus",
    "rose", "lily", "daisy", "tulip", "sunflower", "orchid", "violet", "jasmine",
    "dog", "cat", "bird", "fish", "horse", "cow", "pig", "sheep",
    "chicken", "duck", "rabbit", "hamster", "turtle", "snake", "lizard", "frog",
    "bear", "wolf", "fox", "deer", "moose", "elk", "lion", "tiger",
    "elephant", "giraffe", "zebra", "monkey", "gorilla", "chimpanzee", "panda", "koala",
    "whale", "dolphin", "shark", "octopus", "jellyfish", "starfish", "seahorse", "penguin",
    "eagle", "hawk", "owl", "parrot", "crow", "sparrow", "pigeon", "swan",
    "butterfly", "bee", "ant", "spider", "mosquito", "fly", "beetle", "cricket",
]

HOUSEHOLD = [
    "house", "apartment", "condo", "townhouse", "cottage", "cabin", "mansion", "bungalow",
    "room", "bedroom", "bathroom", "kitchen", "living", "dining", "study", "office",
    "garage", "basement", "attic", "porch", "patio", "deck", "yard", "garden",
    "furniture", "sofa", "couch", "chair", "table", "desk", "shelf", "cabinet",
    "bed", "mattress", "pillow", "blanket", "sheet", "towel", "curtain", "carpet",
    "lamp", "fan", "heater", "thermostat", "dishwasher", "microwave", "blender", "toaster",
    "vacuum", "broom", "mop", "bucket", "sponge", "detergent", "bleach", "soap",
    "trash", "recycling", "compost", "plumbing", "wiring", "insulation", "renovation", "maintenance",
]

MISC_COMMON = [
    "thing", "stuff", "something", "anything", "everything", "nothing", "whatever", "whenever",
    "wherever", "however", "whoever", "whichever", "anyway", "anywhere", "everywhere", "nowhere",
    "always", "never", "sometimes", "often", "usually", "rarely", "seldom", "occasionally",
    "already", "still", "yet", "soon", "later", "earlier", "recently", "lately",
    "enough", "quite", "rather", "fairly", "pretty", "somewhat", "slightly", "hardly",
    "please", "thanks", "sorry", "excuse", "pardon", "welcome", "congratulations", "cheers",
    "mr", "mrs", "ms", "dr", "sir", "madam", "professor", "captain",
    "yes", "no", "maybe", "perhaps", "probably", "possibly", "certainly", "definitely",
    "absolutely", "exactly", "actually", "basically", "honestly", "seriously", "literally", "obviously",
    "apparently", "supposedly", "allegedly", "presumably", "hopefully", "unfortunately", "fortunately", "surprisingly",
    "suddenly", "immediately", "eventually", "gradually", "finally", "meanwhile", "otherwise", "instead",
    "therefore", "however", "moreover", "furthermore", "nevertheless", "nonetheless", "regardless", "regardless",
    "although", "though", "despite", "unless", "whether", "whereas", "whereby", "wherein",
    "etc", "eg", "ie", "vs", "via", "per", "aka", "misc",
]

# ============================================================
# BIGRAMS — systematic generation
# ============================================================

BIGRAMS_CURATED = [
    # Determiners + nouns / function word combos (high freq)
    ("of", "the", 50000), ("in", "the", 45000), ("to", "the", 30000),
    ("on", "the", 25000), ("and", "the", 24000), ("for", "the", 22000),
    ("at", "the", 20000), ("to", "be", 19000), ("in", "a", 18000),
    ("of", "a", 17000), ("it", "is", 16500), ("it", "was", 16000),
    ("i", "am", 15500), ("i", "have", 15000), ("i", "was", 14500),
    ("i", "will", 14000), ("do", "not", 13500), ("he", "was", 13000),
    ("he", "is", 12800), ("she", "was", 12500), ("she", "is", 12200),
    ("that", "the", 12000), ("with", "the", 11800), ("is", "a", 11500),
    ("is", "the", 11200), ("was", "a", 11000), ("was", "the", 10800),
    ("from", "the", 10500), ("by", "the", 10200), ("as", "a", 10000),
    ("will", "be", 9800), ("has", "been", 9500), ("have", "been", 9200),
    ("can", "be", 9000), ("would", "be", 8800), ("there", "is", 8600),
    ("there", "are", 8400), ("there", "was", 8200), ("this", "is", 8000),
    ("that", "is", 7800), ("they", "are", 7600), ("they", "have", 7400),
    ("they", "were", 7200), ("we", "are", 7000), ("we", "have", 6800),
    ("we", "will", 6600), ("you", "are", 6400), ("you", "have", 6200),
    ("you", "can", 6000), ("you", "will", 5800),
    # I + verb patterns
    ("i", "think", 5600), ("i", "know", 5500), ("i", "want", 5400),
    ("i", "need", 5300), ("i", "love", 5200), ("i", "like", 5100),
    ("i", "can", 5000), ("i", "do", 4900), ("i", "would", 4800),
    ("i", "could", 4700), ("i", "should", 4600), ("i", "don't", 4500),
    ("i", "didn't", 4400), ("i", "got", 4300), ("i", "had", 4200),
    ("i", "feel", 4100), ("i", "just", 4000), ("i", "see", 3900),
    ("i", "mean", 3800), ("i", "hope", 3700), ("i", "wish", 3600),
    ("i", "believe", 3500), ("i", "remember", 3400), ("i", "guess", 3300),
    ("i", "thought", 3200), ("i", "really", 3100), ("i", "never", 3000),
    ("i", "always", 2900), ("i", "still", 2800), ("i", "might", 2700),
    ("i", "must", 2600), ("i", "may", 2500),
    # Verb phrases
    ("going", "to", 9500), ("want", "to", 9000), ("need", "to", 8500),
    ("have", "to", 8200), ("used", "to", 8000), ("able", "to", 7800),
    ("got", "to", 7500), ("try", "to", 7200), ("like", "to", 7000),
    ("had", "to", 6800), ("get", "to", 6600), ("came", "to", 6400),
    ("went", "to", 6200), ("back", "to", 6000), ("up", "to", 5800),
    ("come", "on", 5600), ("go", "to", 5500), ("get", "out", 5400),
    ("come", "back", 5300), ("go", "back", 5200), ("look", "at", 5100),
    ("look", "like", 5000), ("come", "from", 4900), ("go", "out", 4800),
    ("get", "up", 4700), ("get", "in", 4600), ("come", "in", 4500),
    ("go", "on", 4400), ("take", "care", 4300), ("find", "out", 4200),
    ("give", "up", 4100), ("come", "up", 4000), ("turn", "out", 3900),
    ("set", "up", 3800), ("pick", "up", 3700), ("put", "on", 3600),
    ("turn", "on", 3500), ("turn", "off", 3400), ("look", "for", 3300),
    ("work", "out", 3200), ("talk", "about", 3100), ("think", "about", 3000),
    ("start", "to", 2900), ("begin", "to", 2800), ("seem", "to", 2700),
    ("tend", "to", 2600), ("learn", "to", 2500), ("love", "to", 2400),
    ("hate", "to", 2300), ("choose", "to", 2200), ("decide", "to", 2100),
    ("plan", "to", 2000), ("hope", "to", 1900), ("expect", "to", 1800),
    ("fail", "to", 1700), ("refuse", "to", 1600), ("agree", "to", 1500),
    ("continue", "to", 1400), ("manage", "to", 1300), ("happen", "to", 1200),
    # Common collocations
    ("thank", "you", 12000), ("right", "now", 8000), ("last", "night", 7500),
    ("good", "morning", 7000), ("good", "night", 6800), ("good", "luck", 6500),
    ("of", "course", 6300), ("at", "least", 6000), ("at", "all", 5800),
    ("so", "much", 5600), ("so", "far", 5400), ("so", "many", 5200),
    ("each", "other", 5000), ("no", "one", 4800), ("a", "lot", 4600),
    ("such", "as", 4400), ("as", "well", 4200), ("more", "than", 4000),
    ("as", "much", 3800), ("at", "home", 3600), ("at", "work", 3400),
    ("at", "school", 3200), ("at", "first", 3100), ("at", "last", 3000),
    ("in", "order", 2900), ("in", "fact", 2800), ("in", "front", 2700),
    ("next", "to", 2600), ("out", "of", 9000), ("a", "few", 2500),
    ("a", "little", 2400), ("one", "of", 8000), ("some", "of", 7500),
    ("most", "of", 7200), ("all", "of", 7000), ("many", "of", 6800),
    ("part", "of", 6500), ("kind", "of", 6200), ("sort", "of", 6000),
    ("lot", "of", 5800), ("end", "of", 5600), ("rest", "of", 5400),
    ("top", "of", 5200), ("number", "of", 5000), ("because", "of", 4800),
    ("instead", "of", 4600), ("front", "of", 4400), ("middle", "of", 4200),
    ("beginning", "of", 4000), ("side", "of", 3800), ("type", "of", 3600),
    ("pair", "of", 3400), ("group", "of", 3200), ("full", "of", 3000),
    # Adjective + noun
    ("new", "york", 4500), ("united", "states", 4200), ("high", "school", 4000),
    ("last", "year", 3800), ("next", "year", 3600), ("long", "time", 3400),
    ("first", "time", 3200), ("real", "estate", 3000), ("young", "man", 2800),
    ("old", "man", 2600), ("good", "time", 2500), ("same", "time", 2400),
    ("right", "thing", 2300), ("last", "time", 2200), ("other", "people", 2100),
    ("new", "year", 2000), ("good", "thing", 1900), ("little", "bit", 1800),
    ("big", "deal", 1700), ("whole", "thing", 1600), ("long", "way", 1500),
    ("real", "life", 1400), ("hard", "work", 1300), ("best", "way", 1200),
    ("social", "media", 1100), ("mental", "health", 1050), ("climate", "change", 1000),
    # Time expressions
    ("right", "away", 2000), ("every", "day", 1900), ("every", "time", 1800),
    ("next", "time", 1700), ("this", "time", 1600), ("last", "week", 1500),
    ("next", "week", 1400), ("this", "week", 1300), ("last", "month", 1200),
    ("every", "year", 1100), ("this", "morning", 1050), ("this", "year", 1000),
    # Texting / casual
    ("i'm", "sorry", 3000), ("i'm", "going", 2800), ("i'm", "not", 2600),
    ("don't", "know", 2500), ("don't", "want", 2400), ("don't", "think", 2300),
    ("don't", "have", 2200), ("don't", "need", 2100), ("don't", "like", 2000),
    ("can't", "wait", 1900), ("can't", "believe", 1800), ("let's", "go", 1700),
    ("come", "on", 1600), ("how", "are", 5000), ("how", "about", 4500),
    ("how", "much", 4000), ("how", "many", 3800), ("how", "long", 3600),
    ("what", "is", 4500), ("what", "are", 4200), ("what", "do", 4000),
    ("what", "about", 3800), ("what", "if", 3600), ("where", "is", 3500),
    ("where", "are", 3300), ("when", "i", 3100), ("when", "you", 3000),
    ("who", "is", 2800), ("why", "do", 2600), ("why", "is", 2400),
    # Preposition phrases
    ("according", "to", 3000), ("due", "to", 2800), ("close", "to", 2600),
    ("similar", "to", 2400), ("related", "to", 2200), ("compared", "to", 2000),
    ("up", "and", 1800), ("down", "the", 1700), ("into", "the", 5000),
    ("through", "the", 4800), ("over", "the", 4600), ("under", "the", 4400),
    ("around", "the", 4200), ("along", "the", 4000), ("across", "the", 3800),
    ("between", "the", 3600), ("behind", "the", 3400), ("before", "the", 3200),
    ("after", "the", 3000), ("about", "the", 2800), ("against", "the", 2600),
    ("without", "the", 2400), ("within", "the", 2200), ("during", "the", 2000),
    ("upon", "the", 1800), ("toward", "the", 1600), ("beyond", "the", 1400),
    ("inside", "the", 1200), ("outside", "the", 1100), ("above", "the", 1000),
    ("below", "the", 900), ("beside", "the", 800), ("beneath", "the", 700),
    # More verb + preposition
    ("think", "of", 2500), ("ask", "for", 2300),
    ("wait", "for", 2200), ("listen", "to", 2100), ("talk", "to", 2000),
    ("belong", "to", 1900), ("lead", "to", 1700),
    ("refer", "to", 1600), ("apply", "to", 1500), ("point", "to", 1400),
    ("agree", "with", 1300), ("deal", "with", 1200), ("live", "with", 1100),
    ("work", "with", 1000), ("play", "with", 950), ("start", "with", 900),
    ("end", "with", 850), ("come", "with", 800), ("go", "with", 750),
    ("depend", "on", 700), ("focus", "on", 650), ("based", "on", 600),
    ("work", "on", 550), ("move", "on", 500), ("hold", "on", 480),
    ("carry", "on", 460), ("keep", "on", 440), ("pass", "on", 420),
    # To + verb
    ("to", "do", 4000), ("to", "get", 3800), ("to", "make", 3600),
    ("to", "go", 3400), ("to", "take", 3200), ("to", "come", 3000),
    ("to", "see", 2800), ("to", "know", 2600), ("to", "have", 2400),
    ("to", "find", 2200), ("to", "give", 2000), ("to", "say", 1800),
    ("to", "use", 1600), ("to", "keep", 1400), ("to", "help", 1200),
    ("to", "work", 1100), ("to", "start", 1000), ("to", "think", 950),
    ("to", "talk", 900), ("to", "tell", 850), ("to", "put", 800),
    ("to", "try", 750), ("to", "leave", 700), ("to", "feel", 650),
    ("to", "play", 600), ("to", "run", 550), ("to", "read", 500),
    ("to", "write", 480), ("to", "learn", 460), ("to", "change", 440),
    ("to", "move", 420), ("to", "live", 400), ("to", "stop", 380),
    ("to", "pay", 360), ("to", "meet", 340), ("to", "eat", 320),
    ("to", "buy", 300), ("to", "build", 280), ("to", "spend", 260),
    ("to", "win", 240), ("to", "lose", 220), ("to", "bring", 200),
    # Subject + verb
    ("he", "said", 5000), ("she", "said", 4800), ("they", "said", 4600),
    ("he", "had", 4500), ("she", "had", 4300), ("they", "had", 4100),
    ("he", "could", 4000), ("she", "could", 3800), ("they", "could", 3600),
    ("he", "would", 3500), ("she", "would", 3300), ("they", "would", 3100),
    ("it", "would", 3000), ("it", "could", 2800), ("it", "has", 2600),
    ("it", "had", 2500), ("it", "will", 2400), ("it", "can", 2300),
    ("we", "can", 2200), ("we", "need", 2100), ("we", "should", 2000),
    ("we", "could", 1900), ("we", "would", 1800), ("we", "must", 1700),
    ("you", "know", 1600), ("you", "want", 1500), ("you", "need", 1400),
    ("you", "should", 1300), ("you", "could", 1200), ("you", "think", 1100),
    ("you", "don't", 1050), ("you", "get", 1000),
    # Good/bad + noun
    ("good", "idea", 1800), ("bad", "idea", 1200), ("good", "news", 1700),
    ("bad", "news", 1100), ("good", "job", 1600), ("good", "day", 1500),
    ("bad", "day", 1000), ("good", "food", 1400), ("good", "people", 1300),
    ("happy", "birthday", 2500), ("merry", "christmas", 2000), ("happy", "new", 1800),
    # Noun + noun
    ("ice", "cream", 1500), ("high", "school", 1400), ("living", "room", 1300),
    ("phone", "number", 1200), ("credit", "card", 1100), ("email", "address", 1000),
    ("health", "care", 950), ("real", "world", 900), ("video", "game", 850),
    ("cell", "phone", 800), ("web", "site", 750), ("bus", "stop", 700),
]

# ============================================================
# TRIGRAMS — curated
# ============================================================

TRIGRAMS_CURATED = [
    # of/in/to the + noun
    ("one", "of", "the", 5000), ("out", "of", "the", 4500),
    ("some", "of", "the", 4000), ("part", "of", "the", 3800),
    ("end", "of", "the", 3600), ("all", "of", "the", 3500),
    ("most", "of", "the", 3400), ("because", "of", "the", 3200),
    ("in", "front", "of", 3000), ("a", "lot", "of", 2900),
    ("rest", "of", "the", 2800), ("top", "of", "the", 2700),
    ("side", "of", "the", 2600), ("back", "of", "the", 2500),
    ("middle", "of", "the", 2400), ("beginning", "of", "the", 2300),
    ("kind", "of", "the", 2200), ("number", "of", "the", 2100),
    # Pronoun patterns
    ("i", "don't", "know", 4500), ("i", "don't", "want", 4200),
    ("i", "don't", "think", 4000), ("i", "don't", "have", 3800),
    ("i", "don't", "like", 3600), ("i", "don't", "need", 3400),
    ("i", "want", "to", 4000), ("i", "need", "to", 3800),
    ("i", "have", "to", 3600), ("i", "used", "to", 3400),
    ("i", "have", "been", 3200), ("i", "would", "like", 3000),
    ("i", "think", "it", 2800), ("i", "think", "that", 2600),
    ("i", "think", "we", 2400), ("i", "think", "you", 2200),
    ("i", "am", "going", 2100), ("i", "am", "not", 2000),
    ("i", "am", "a", 1900), ("i", "am", "so", 1800),
    ("i", "was", "going", 1700), ("i", "was", "just", 1600),
    ("i", "was", "like", 1500), ("i", "was", "a", 1400),
    ("i", "will", "be", 1300), ("i", "can", "not", 1200),
    ("i", "could", "not", 1100), ("i", "would", "not", 1050),
    ("i", "hope", "you", 1000), ("i", "hope", "that", 950),
    ("i", "wish", "i", 900), ("i", "feel", "like", 850),
    ("i", "love", "you", 3000), ("i", "love", "it", 2000),
    ("i", "love", "this", 1500), ("i", "miss", "you", 1200),
    ("i", "told", "you", 1100), ("i", "told", "him", 1000),
    ("i", "told", "her", 950), ("i", "asked", "him", 900),
    ("i", "asked", "her", 850),
    # it is/was
    ("it", "is", "a", 3500), ("it", "is", "not", 3300),
    ("it", "is", "the", 3100), ("it", "was", "a", 3000),
    ("it", "was", "the", 2800), ("it", "was", "not", 2600),
    ("it", "will", "be", 2400), ("it", "would", "be", 2200),
    ("it", "can", "be", 2000), ("it", "could", "be", 1800),
    ("it", "has", "been", 1600), ("it", "had", "been", 1400),
    # there is/are
    ("there", "is", "a", 3000), ("there", "is", "no", 2800),
    ("there", "are", "many", 2600), ("there", "are", "some", 2400),
    ("there", "was", "a", 2200), ("there", "was", "no", 2000),
    ("there", "were", "no", 1800), ("there", "will", "be", 1600),
    # this/that is
    ("this", "is", "a", 2500), ("this", "is", "the", 2400),
    ("this", "is", "not", 2300), ("that", "is", "the", 2200),
    ("that", "is", "a", 2100), ("that", "is", "not", 2000),
    ("that", "is", "why", 1900), ("that", "is", "how", 1800),
    # Verb phrase patterns
    ("going", "to", "be", 3000), ("going", "to", "do", 2800),
    ("going", "to", "get", 2600), ("going", "to", "have", 2400),
    ("going", "to", "make", 2200), ("going", "to", "take", 2000),
    ("want", "to", "be", 2800), ("want", "to", "do", 2600),
    ("want", "to", "get", 2400), ("want", "to", "go", 2200),
    ("want", "to", "know", 2000), ("want", "to", "make", 1800),
    ("want", "to", "see", 1600), ("need", "to", "be", 2400),
    ("need", "to", "do", 2200), ("need", "to", "get", 2000),
    ("need", "to", "know", 1800), ("need", "to", "have", 1600),
    ("have", "to", "be", 2200), ("have", "to", "do", 2000),
    ("have", "to", "go", 1800), ("have", "to", "get", 1600),
    ("try", "to", "get", 1500), ("try", "to", "find", 1400),
    ("try", "to", "make", 1300), ("try", "to", "be", 1200),
    ("like", "to", "be", 1100), ("like", "to", "have", 1050),
    ("like", "to", "do", 1000), ("able", "to", "do", 950),
    ("able", "to", "get", 900), ("able", "to", "make", 850),
    # has/have been
    ("has", "been", "a", 2000), ("has", "been", "the", 1800),
    ("have", "been", "a", 1700), ("have", "been", "the", 1600),
    ("had", "been", "a", 1500), ("had", "been", "the", 1400),
    ("will", "be", "a", 1300), ("will", "be", "the", 1200),
    ("would", "be", "a", 1100), ("would", "be", "the", 1000),
    ("can", "be", "a", 950), ("could", "be", "a", 900),
    # Question patterns
    ("what", "do", "you", 2500), ("what", "is", "the", 2400),
    ("what", "is", "it", 2300), ("what", "are", "you", 2200),
    ("what", "if", "i", 2100), ("what", "if", "we", 2000),
    ("how", "do", "you", 2400), ("how", "are", "you", 2300),
    ("how", "much", "is", 2200), ("how", "many", "people", 2100),
    ("how", "long", "is", 2000), ("how", "about", "we", 1900),
    ("where", "is", "the", 1800), ("where", "are", "you", 1700),
    ("where", "do", "you", 1600), ("when", "i", "was", 1500),
    ("when", "it", "comes", 1400), ("when", "you", "are", 1300),
    ("who", "is", "the", 1200), ("why", "do", "you", 1100),
    ("why", "is", "it", 1050), ("why", "don't", "you", 1000),
    # Common 3-word phrases
    ("as", "well", "as", 2500), ("a", "little", "bit", 2000),
    ("more", "and", "more", 1800), ("at", "the", "same", 1700),
    ("the", "same", "time", 1600), ("at", "the", "end", 1500),
    ("at", "the", "time", 1400), ("on", "the", "other", 1300),
    ("the", "other", "hand", 1200), ("for", "the", "first", 1100),
    ("the", "first", "time", 1050), ("for", "a", "long", 1000),
    ("a", "long", "time", 950), ("in", "the", "first", 900),
    ("in", "the", "same", 850), ("in", "the", "world", 800),
    ("in", "the", "end", 750), ("in", "the", "morning", 700),
    ("in", "the", "middle", 650), ("in", "the", "past", 600),
    ("in", "the", "future", 550), ("on", "the", "way", 500),
    ("by", "the", "way", 460), ("by", "the", "time", 440),
    ("for", "the", "last", 420), ("for", "the", "next", 400),
    ("to", "the", "point", 380), ("up", "to", "the", 360),
    ("out", "of", "my", 340), ("out", "of", "his", 320),
    ("out", "of", "her", 300),
    # Texting / casual
    ("i'm", "going", "to", 2000), ("i'm", "not", "sure", 1800),
    ("i'm", "not", "going", 1600), ("don't", "want", "to", 1500),
    ("don't", "have", "to", 1400), ("don't", "need", "to", 1300),
    ("don't", "know", "what", 1200), ("don't", "know", "how", 1100),
    ("don't", "know", "if", 1050), ("don't", "think", "so", 1000),
    ("can't", "wait", "to", 950), ("let", "me", "know", 900),
    ("thank", "you", "for", 2500), ("thank", "you", "so", 2000),
    ("you", "know", "what", 850), ("you", "know", "i", 800),
    ("you", "want", "to", 750), ("you", "need", "to", 700),
    ("you", "have", "to", 650), ("you", "don't", "have", 600),
    ("you", "don't", "know", 550), ("you", "don't", "want", 500),
    ("see", "you", "later", 1500), ("see", "you", "soon", 1400),
    ("nice", "to", "meet", 1300), ("have", "a", "good", 1200),
    ("have", "a", "great", 1100), ("have", "a", "nice", 1000),
    ("take", "care", "of", 950), ("look", "forward", "to", 900),
    ("come", "up", "with", 850),
    # More structural
    ("the", "end", "of", 800), ("the", "rest", "of", 750),
    ("the", "beginning", "of", 700), ("the", "top", "of", 650),
    ("the", "middle", "of", 600), ("the", "back", "of", 550),
    ("the", "front", "of", 500), ("the", "side", "of", 480),
    ("the", "bottom", "of", 460), ("the", "number", "of", 440),
    ("the", "kind", "of", 420), ("the", "fact", "that", 400),
    ("the", "way", "you", 380), ("the", "way", "it", 360),
    ("the", "way", "i", 340), ("the", "way", "we", 320),
    ("in", "order", "to", 1500), ("as", "long", "as", 1400),
    ("as", "soon", "as", 1300), ("as", "much", "as", 1200),
    ("as", "far", "as", 1100), ("as", "if", "it", 1000),
    ("such", "as", "the", 950), ("so", "that", "the", 900),
    ("so", "that", "we", 850), ("so", "that", "you", 800),
    ("even", "if", "you", 750), ("even", "if", "it", 700),
    ("more", "than", "a", 650), ("more", "than", "the", 600),
    ("less", "than", "a", 550), ("up", "to", "you", 500),
    # Subject + verb + object
    ("he", "said", "he", 800), ("she", "said", "she", 750),
    ("they", "said", "they", 700), ("he", "told", "me", 650),
    ("she", "told", "me", 600), ("they", "told", "me", 550),
    # we/they patterns
    ("we", "need", "to", 1500), ("we", "have", "to", 1400),
    ("we", "want", "to", 1300), ("we", "are", "going", 1200),
    ("we", "can", "do", 1100), ("we", "should", "be", 1050),
    ("they", "want", "to", 1000), ("they", "need", "to", 950),
    ("they", "have", "to", 900), ("they", "are", "going", 850),
    ("they", "don't", "want", 800), ("they", "don't", "know", 750),
    # Additional patterns
    ("do", "you", "want", 1200), ("do", "you", "think", 1100),
    ("do", "you", "know", 1050), ("do", "you", "have", 1000),
    ("do", "you", "like", 950), ("do", "you", "need", 900),
    ("do", "you", "remember", 850), ("do", "you", "mean", 800),
    ("did", "you", "know", 750), ("did", "you", "get", 700),
    ("did", "you", "see", 650), ("did", "you", "hear", 600),
    ("can", "you", "please", 550), ("can", "you", "help", 500),
    ("can", "you", "tell", 480), ("can", "you", "do", 460),
    ("would", "you", "like", 1000), ("would", "you", "mind", 800),
    ("could", "you", "please", 750), ("should", "you", "need", 400),
    ("let", "me", "know", 1500), ("let", "me", "see", 800),
    ("let", "me", "think", 750), ("let", "me", "get", 700),
    ("let", "us", "know", 650), ("let", "me", "help", 600),
    # Time and place
    ("at", "the", "moment", 500), ("at", "the", "beginning", 480),
    ("in", "the", "meantime", 460), ("for", "the", "time", 440),
    ("in", "the", "next", 420), ("at", "the", "top", 400),
    ("on", "the", "right", 380), ("on", "the", "left", 360),
    ("in", "the", "back", 340), ("at", "the", "bottom", 320),
    ("on", "the", "phone", 300), ("in", "the", "car", 280),
    ("at", "the", "door", 260), ("in", "the", "house", 240),
    ("on", "the", "table", 220), ("in", "the", "kitchen", 200),
]


def generate_unigrams():
    """Generate ~10,000 unigrams with Zipf-distributed frequencies."""
    seen = set()
    lines = []

    def add(word, freq):
        w = word.strip().lower()
        if w and w not in seen and len(w) <= 30:
            seen.add(w)
            lines.append(f"{w} {freq}")

    # High-frequency curated words
    for word, freq in TOP_WORDS:
        add(word, freq)
    for word, freq in COMMON_WORDS:
        add(word, freq)
    for word, freq in HIGH_FREQ:
        add(word, freq)
    for word, freq in MED_FREQ_WORDS:
        add(word, freq)

    # Category word lists with decreasing frequency tiers
    categories = [
        (NOUNS, 1500, 100),
        (VERBS, 1200, 80),
        (ADJECTIVES, 1000, 60),
        (ADVERBS, 800, 50),
        (PREPOSITIONS, 2000, 500),
        (CONJUNCTIONS, 1500, 300),
        (PRONOUNS, 2500, 200),
        (NUMBERS_AND_ORDINALS, 1800, 200),
        (TECH_AND_INTERNET, 600, 50),
        (CASUAL_AND_TEXTING, 500, 30),
        (DAYS_MONTHS_TIME, 1200, 100),
        (COUNTRIES_AND_PLACES, 400, 30),
        (EMOTIONS_AND_EXPRESSIONS, 500, 40),
        (BUSINESS_AND_WORK, 400, 30),
        (HEALTH_AND_WELLNESS, 350, 25),
        (EDUCATION, 350, 25),
        (FOOD_EXTRA, 300, 20),
        (ENTERTAINMENT, 350, 25),
        (TRAVEL, 300, 20),
        (SPORTS, 300, 20),
        (NATURE_AND_ANIMALS, 250, 15),
        (HOUSEHOLD, 300, 20),
        (MISC_COMMON, 500, 40),
    ]

    for word_list, max_freq, min_freq in categories:
        n = len(word_list)
        for i, word in enumerate(word_list):
            # Zipf-like decay within each category
            freq = max(min_freq, int(max_freq / (1 + i * 0.5)))
            add(word, freq)

    # Generate word forms (plurals, past tense, -ing, -ly, -er, -est, -tion, -ment, -ness)
    base_words = [
        "accept", "access", "account", "achieve", "act", "add", "address", "adjust", "advance", "affect",
        "allow", "announce", "answer", "appear", "apply", "approach", "approve", "arrange", "arrive", "assume",
        "attach", "attack", "attempt", "attend", "attract", "avoid", "balance", "base", "beat", "belong",
        "benefit", "block", "blow", "board", "borrow", "bother", "bounce", "brand", "break", "breathe",
        "broadcast", "brush", "budget", "burn", "calculate", "camp", "capture", "celebrate", "challenge", "charge",
        "chase", "cheat", "circle", "claim", "climb", "collect", "comfort", "command", "commit", "compare",
        "compete", "complain", "complete", "concern", "condition", "conduct", "confess", "confirm", "conflict", "connect",
        "consider", "consist", "construct", "consume", "contact", "contain", "content", "contest", "contract", "contribute",
        "control", "convert", "convince", "correct", "count", "cover", "crash", "cross", "crowd", "cure",
        "damage", "deal", "debate", "decline", "defend", "define", "delay", "deliver", "demand", "demonstrate",
        "deposit", "describe", "design", "desire", "destroy", "detect", "determine", "develop", "differ", "direct",
        "disappear", "discover", "display", "distance", "distribute", "divide", "document", "doubt", "draft", "dream",
        "drift", "drop", "earn", "edit", "educate", "elect", "eliminate", "embrace", "emerge", "employ",
        "enable", "encourage", "engage", "engineer", "enjoy", "ensure", "enter", "establish", "evaluate", "examine",
        "exchange", "excite", "execute", "exercise", "exhibit", "exist", "expand", "expect", "experience", "experiment",
        "explain", "explore", "export", "expose", "extend", "face", "factor", "fail", "fashion", "favor",
        "feature", "figure", "file", "fill", "film", "filter", "finance", "finish", "fit", "fix",
        "flash", "float", "flow", "fly", "fold", "force", "form", "format", "found", "frame",
        "fuel", "function", "gain", "gather", "generate", "grab", "grade", "grant", "grip", "ground",
        "guarantee", "guard", "handle", "harm", "head", "highlight", "host", "house", "hunt", "hurry",
        "identify", "illustrate", "imagine", "implement", "import", "impose", "impress", "improve", "include", "increase",
        "indicate", "influence", "inform", "injure", "insert", "inspect", "install", "institute", "instruct", "insure",
        "intend", "interact", "interest", "interpret", "introduce", "invest", "investigate", "involve", "issue", "isolate",
        "judge", "jump", "justify", "label", "land", "last", "launch", "layer", "lean", "lecture",
        "level", "license", "lift", "limit", "link", "list", "load", "locate", "log", "lower",
        "maintain", "manage", "manufacture", "mark", "market", "master", "match", "matter", "measure", "mention",
        "merge", "mind", "minister", "miss", "model", "modify", "monitor", "mount", "narrow", "negotiate",
        "network", "note", "notice", "object", "observe", "obtain", "occupy", "occur", "operate", "oppose",
        "option", "order", "organize", "outline", "overcome", "overlook", "own", "pack", "pair", "park",
        "participate", "partner", "pass", "patch", "pause", "peak", "perform", "permit", "persist", "persuade",
        "photograph", "pick", "pile", "pitch", "plant", "please", "plot", "plug", "pocket", "poll",
        "pool", "pop", "position", "post", "pour", "praise", "predict", "prefer", "prepare", "preserve",
        "press", "pretend", "prevent", "pride", "print", "proceed", "process", "produce", "profile", "progress",
        "project", "promote", "prompt", "propose", "protect", "protest", "prove", "purchase", "pursue", "push",
        "qualify", "question", "quote", "race", "rain", "raise", "range", "rank", "rate", "reach",
        "react", "realize", "reason", "receive", "recognize", "recommend", "record", "recover", "reduce", "reflect",
        "reform", "refuse", "regard", "register", "regulate", "reject", "relate", "release", "rely", "remain",
        "remark", "remove", "renew", "rent", "repair", "repeat", "replace", "reply", "report", "represent",
        "request", "require", "research", "reserve", "resign", "resist", "resolve", "resource", "respond", "restore",
        "restrict", "result", "retain", "retire", "reveal", "review", "risk", "roll", "root", "rotate",
        "round", "rush", "sample", "satisfy", "save", "scale", "scan", "schedule", "score", "screen",
        "search", "secure", "seek", "select", "separate", "serve", "settle", "shape", "share", "shelter",
        "shift", "ship", "shock", "shoot", "shop", "signal", "sign", "silence", "sink", "slip",
        "slow", "smoke", "snap", "solve", "sort", "source", "spark", "speak", "speed", "split",
        "sponsor", "spot", "spread", "squeeze", "stage", "stand", "star", "state", "station", "stay",
        "step", "stick", "stock", "store", "storm", "strain", "stretch", "strike", "strip", "struggle",
        "structure", "study", "style", "submit", "succeed", "suffer", "suggest", "suit", "supply", "support",
        "suppose", "surface", "surprise", "surround", "survey", "survive", "suspect", "sustain", "switch", "target",
        "taste", "tax", "teach", "tear", "test", "thank", "threaten", "throw", "tie", "tip",
        "title", "touch", "tour", "track", "trade", "train", "transfer", "transform", "translate", "transport",
        "trap", "travel", "treat", "trend", "trigger", "trim", "trip", "trouble", "trust", "tune",
        "twist", "type", "undergo", "underline", "understand", "undertake", "unite", "update", "upgrade", "urge",
        "value", "vary", "venture", "view", "visit", "voice", "volunteer", "vote", "wage", "walk",
        "wander", "warn", "wash", "waste", "wave", "weigh", "welcome", "whisper", "wonder", "worry",
        "wound", "wrap", "yield", "zone",
    ]

    suffixes = [
        ("ed", 400), ("ing", 500), ("s", 600), ("er", 300), ("est", 200),
        ("ly", 350), ("ment", 250), ("ness", 200), ("tion", 300), ("able", 200),
    ]

    for base in base_words:
        add(base, 600)
        for suffix, base_freq in suffixes:
            # Simple suffix attachment (not linguistically perfect, but good enough for keyboard data)
            if suffix == "ed":
                if base.endswith("e"):
                    form = base + "d"
                elif base.endswith("y") and len(base) > 2 and base[-2] not in "aeiou":
                    form = base[:-1] + "ied"
                else:
                    form = base + suffix
            elif suffix == "ing":
                if base.endswith("e") and not base.endswith("ee"):
                    form = base[:-1] + suffix
                else:
                    form = base + suffix
            elif suffix == "s":
                if base.endswith(("s", "x", "z", "ch", "sh")):
                    form = base + "es"
                elif base.endswith("y") and len(base) > 2 and base[-2] not in "aeiou":
                    form = base[:-1] + "ies"
                else:
                    form = base + suffix
            elif suffix == "er":
                if base.endswith("e"):
                    form = base + "r"
                else:
                    form = base + suffix
            elif suffix == "est":
                if base.endswith("e"):
                    form = base + "st"
                else:
                    form = base + suffix
            elif suffix == "ly":
                if base.endswith("y"):
                    form = base[:-1] + "ily"
                elif base.endswith("le"):
                    form = base[:-1] + "y"
                else:
                    form = base + suffix
            elif suffix in ("tion", "ment", "ness", "able"):
                if base.endswith("e"):
                    form = base[:-1] + suffix
                else:
                    form = base + suffix
            else:
                form = base + suffix

            freq = max(50, base_freq - random.randint(0, 100))
            add(form, freq)

    return lines


def generate_bigrams():
    """Generate ~5,000 bigrams."""
    seen = set()
    lines = []

    def add(w1, w2, freq):
        key = f"{w1.lower()} {w2.lower()}"
        if key not in seen:
            seen.add(key)
            lines.append(f"{w1.lower()} {w2.lower()} {freq}")

    # Curated bigrams
    for w1, w2, freq in BIGRAMS_CURATED:
        add(w1, w2, freq)

    # Programmatic: preposition + common nouns
    common_nouns = [
        "time", "year", "people", "way", "day", "man", "woman", "world", "life",
        "hand", "part", "place", "week", "night", "home", "water", "room", "money",
        "story", "fact", "month", "right", "book", "eye", "job", "word", "business",
        "side", "head", "house", "friend", "power", "hour", "game", "line", "end",
        "car", "city", "name", "team", "idea", "body", "school", "family", "student",
        "country", "problem", "change", "face", "door", "health", "person", "war",
        "reason", "moment", "air", "table", "office", "market", "plan", "age",
        "voice", "phone", "computer", "building", "road", "bed", "music", "food",
        "point", "thing", "group", "class", "morning", "evening", "state", "area",
        "system", "case", "question", "work", "number", "program", "government", "company",
    ]
    preps = ["in", "on", "at", "by", "with", "from", "to", "for", "about", "into",
             "through", "over", "under", "between", "after", "before", "during", "without"]
    for prep in preps:
        for noun in common_nouns:
            freq = max(50, random.randint(100, 800))
            add(prep, noun, freq)

    # Programmatic: adjective + noun
    common_adjs = [
        "good", "new", "first", "last", "long", "great", "little", "old", "big", "high",
        "small", "large", "next", "young", "important", "public", "bad", "same", "right",
        "real", "best", "free", "special", "easy", "clear", "recent", "certain", "personal",
        "whole", "strong", "true", "fast", "short", "dark", "low", "main", "happy",
        "local", "nice", "hot", "cold", "clean", "fresh", "beautiful", "wonderful", "amazing",
        "perfect", "simple", "hard", "safe", "different", "possible", "social", "current",
    ]
    for adj in common_adjs:
        for noun in common_nouns[:40]:  # top 40 nouns
            freq = max(50, random.randint(50, 500))
            add(adj, noun, freq)

    # Programmatic: verb + preposition/particle
    common_verbs = [
        "go", "come", "get", "take", "make", "put", "give", "keep", "let", "run",
        "look", "find", "turn", "bring", "try", "set", "move", "play", "work", "call",
        "help", "ask", "show", "start", "leave", "talk", "think", "read", "write", "live",
        "open", "close", "pull", "push", "sit", "stand", "walk", "check", "pick", "cut",
        "break", "fall", "hold", "send", "build", "pay", "buy", "sell", "eat", "drink",
    ]
    particles = ["up", "out", "in", "on", "off", "down", "over", "back", "away", "about",
                 "around", "through", "along", "together", "apart", "ahead", "forward"]
    for verb in common_verbs:
        for particle in particles:
            freq = max(50, random.randint(80, 600))
            add(verb, particle, freq)

    # Programmatic: subject + verb
    subjects = ["i", "you", "he", "she", "it", "we", "they", "who", "that", "this",
                "people", "someone", "everyone", "nobody", "anybody"]
    verbs_for_subj = [
        "want", "need", "like", "love", "hate", "think", "know", "believe", "feel", "see",
        "hear", "understand", "remember", "forget", "hope", "wish", "expect", "mean", "try",
        "start", "stop", "keep", "find", "make", "take", "get", "give", "tell", "ask",
        "say", "go", "come", "leave", "stay", "live", "work", "play", "read", "write",
    ]
    for subj in subjects:
        for verb in verbs_for_subj:
            freq = max(50, random.randint(100, 800))
            add(subj, verb, freq)

    # Programmatic: determiner + noun
    determiners = ["the", "a", "an", "this", "that", "these", "those", "my", "your", "his",
                   "her", "its", "our", "their", "some", "any", "no", "every", "each"]
    for det in determiners:
        for noun in common_nouns[:50]:
            freq = max(50, random.randint(100, 1000))
            add(det, noun, freq)

    # Programmatic: adverb + adjective
    adverbs_mod = ["very", "really", "so", "too", "quite", "pretty", "fairly", "extremely",
                   "incredibly", "absolutely", "completely", "totally", "particularly", "especially"]
    adjs_mod = ["good", "bad", "big", "small", "long", "short", "fast", "slow", "hard", "easy",
                "happy", "sad", "nice", "important", "different", "interesting", "beautiful", "amazing",
                "wonderful", "terrible", "great", "cool", "awesome", "funny", "strange", "tired",
                "hungry", "busy", "sorry", "sure", "close", "far", "old", "young", "hot", "cold"]
    for adv in adverbs_mod:
        for adj in adjs_mod:
            freq = max(50, random.randint(50, 400))
            add(adv, adj, freq)

    return lines


def generate_trigrams():
    """Generate ~2,000 trigrams."""
    seen = set()
    lines = []

    def add(w1, w2, w3, freq):
        key = f"{w1.lower()} {w2.lower()} {w3.lower()}"
        if key not in seen:
            seen.add(key)
            lines.append(f"{w1.lower()} {w2.lower()} {w3.lower()} {freq}")

    # Curated trigrams
    for w1, w2, w3, freq in TRIGRAMS_CURATED:
        add(w1, w2, w3, freq)

    # Programmatic: prep + the + noun
    preps = ["in", "on", "at", "by", "with", "from", "to", "for", "about",
             "through", "over", "under", "between", "after", "before", "during"]
    nouns = [
        "time", "year", "people", "way", "day", "man", "world", "life",
        "place", "week", "night", "home", "water", "room", "money",
        "story", "fact", "month", "book", "eye", "job", "word",
        "side", "head", "house", "friend", "power", "hour", "game",
        "car", "city", "name", "team", "idea", "body", "school",
        "country", "problem", "face", "door", "person", "morning",
        "table", "office", "market", "plan", "age", "phone", "building",
    ]
    for prep in preps:
        for noun in nouns:
            freq = max(50, random.randint(50, 400))
            add(prep, "the", noun, freq)

    # Programmatic: subject + verb + to
    subjects = ["i", "you", "he", "she", "we", "they"]
    verbs_to = ["want", "need", "have", "like", "love", "try", "hope", "plan",
                "decide", "choose", "learn", "start", "begin", "continue", "refuse",
                "agree", "expect", "seem", "tend", "wish", "hate", "prefer", "fail"]
    for subj in subjects:
        for verb in verbs_to:
            freq = max(50, random.randint(100, 600))
            add(subj, verb, "to", freq)

    # Programmatic: subject + don't/didn't/can't + verb
    neg_aux = ["don't", "didn't", "can't", "won't", "wouldn't", "couldn't", "shouldn't"]
    neg_verbs = ["know", "want", "think", "have", "like", "need", "care",
                 "mind", "understand", "believe", "remember", "forget", "see", "say"]
    for subj in ["i", "you", "we", "they"]:
        for aux in neg_aux:
            for verb in neg_verbs:
                freq = max(50, random.randint(50, 300))
                add(subj, aux, verb, freq)

    # Programmatic: it/this/that + is/was + adj
    copula_subjs = ["it", "this", "that"]
    copulas = ["is", "was"]
    adjs = ["good", "bad", "great", "nice", "important", "hard", "easy", "possible",
            "true", "clear", "obvious", "likely", "enough", "better", "worse", "best",
            "difficult", "simple", "interesting", "amazing", "wonderful", "terrible"]
    for subj in copula_subjs:
        for cop in copulas:
            for adj in adjs:
                freq = max(50, random.randint(50, 300))
                add(subj, cop, adj, freq)

    # Programmatic: have/has/had + been + verb-ing / adj
    have_forms = ["have", "has", "had"]
    been_cont = ["been", "a", "the", "not", "always", "never", "already", "just"]
    for hf in have_forms:
        for cont in been_cont:
            for word in ["good", "great", "there", "here", "long", "able", "doing",
                         "going", "working", "trying", "waiting", "looking", "living"]:
                freq = max(50, random.randint(50, 250))
                add(hf, cont, word, freq)

    # Programmatic: adj + adj + noun (very/really X)
    intens = ["very", "really", "so", "quite", "pretty", "too", "extremely"]
    adj_list = ["good", "bad", "big", "small", "long", "nice", "happy", "important",
                "different", "hard", "easy", "fast", "slow", "old", "young"]
    noun_list = ["thing", "time", "day", "way", "idea", "place", "person", "job"]
    for intens_word in intens:
        for adj in adj_list:
            for noun in noun_list[:5]:  # limit combinations
                freq = max(50, random.randint(50, 200))
                add(intens_word, adj, noun, freq)

    return lines


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    unigrams = generate_unigrams()
    bigrams = generate_bigrams()
    trigrams = generate_trigrams()

    with open(os.path.join(OUTPUT_DIR, "unigrams.txt"), "w") as f:
        f.write("\n".join(unigrams) + "\n")

    with open(os.path.join(OUTPUT_DIR, "bigrams.txt"), "w") as f:
        f.write("\n".join(bigrams) + "\n")

    with open(os.path.join(OUTPUT_DIR, "trigrams.txt"), "w") as f:
        f.write("\n".join(trigrams) + "\n")

    print(f"Generated {len(unigrams)} unigrams")
    print(f"Generated {len(bigrams)} bigrams")
    print(f"Generated {len(trigrams)} trigrams")


if __name__ == "__main__":
    main()
