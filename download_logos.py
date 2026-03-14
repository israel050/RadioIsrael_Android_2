#!/usr/bin/env python3
"""
הרץ פעם אחת לפני ה-build.
מוריד את כל לוגואי התחנות (194 תחנות + 72 ערוצי KCM).
דרישות: python 3.x (אין צורך בחבילות נוספות)
שימוש:  python download_logos.py
"""
import os, json, urllib.request, time

ASSETS_DIR = os.path.join("app", "src", "main", "assets", "logos")
os.makedirs(ASSETS_DIR, exist_ok=True)

HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}

def download(sid, url, is_kcm=False):
    if not url:
        return False
    ext = url.rsplit(".", 1)[-1].split("?")[0].lower()
    if ext not in ("jpg","jpeg","png","webp","gif"):
        ext = "jpg"
    dest = os.path.join(ASSETS_DIR, f"{sid}.{ext}")
    if os.path.exists(dest) and os.path.getsize(dest) > 500:
        return True  # already downloaded
    try:
        req = urllib.request.Request(url, headers=HEADERS)
        with urllib.request.urlopen(req, timeout=12) as r, open(dest, "wb") as out:
            out.write(r.read())
        return True
    except Exception as e:
        if is_kcm:
            # KCM individual logo failed → use shared KCM logo
            shared = os.path.join(ASSETS_DIR, "kcm_shared.jpg")
            if os.path.exists(shared):
                import shutil; shutil.copy(shared, dest)
                return True
        return False

# ── 1. רדיו KCM שיתפותי ───────────────────────────────────────────────────
KCM_SHARED = "https://cdn.rlive.co.il/PJ06OR3WF8OBQWHUJ1PQU9SUR@tb.jpg"
shared_dest = os.path.join(ASSETS_DIR, "kcm_shared.jpg")
if not os.path.exists(shared_dest):
    try:
        req = urllib.request.Request(KCM_SHARED, headers=HEADERS)
        with urllib.request.urlopen(req, timeout=12) as r, open(shared_dest, "wb") as out:
            out.write(r.read())
        print("✅ kcm_shared.jpg")
    except Exception as e:
        print(f"❌ kcm_shared: {e}")

# ── 2. כל התחנות + ערוצי KCM ─────────────────────────────────────────────
ALL_LOGOS = [
    ("96fm", "https://cdn.rlive.co.il/W99GRY2X96GVTJDM5T1IEKC6E@tb.png", False),
    ("ah-fm", "https://cdn.rlive.co.il/01JWEB687S26QCVY2Y9KY3B6YA@tb.jpg", False),
    ("69fm", "https://cdn.rlive.co.il/59HLDOXTW8DHAK0SVZLX0E1YB@tb.png", False),
    ("91fm", "https://cdn.rlive.co.il/EH7FNTQZJDLP3SP2TQZJPYKR4@tb.png", False),
    ("al-hagal", "https://cdn.rlive.co.il/01JH82AS57BQ45SC33Z2254FED@tb.jpg", False),
    ("93fm", "https://cdn.rlive.co.il/KEZ5VEXRDY1WNT4NBQHS0AOKE@tb.jpg", False),
    ("103fm", "https://cdn.rlive.co.il/SQNCCXZUU5YESU7SBQAOKKURT@tb.png", False),
    ("ambient-sleeping-pill", "https://cdn.rlive.co.il/V4AOLB3MF5QTQSW0JFU0O3KCD@tb.png", False),
    ("105-network", "https://cdn.rlive.co.il/SYJH3QNJ6H8TNSHCDR2N3T0V3@tb.jpg", False),
    ("104-5-fm", "https://cdn.rlive.co.il/HZLVLIXRD9BST0EKZXAUYXSFU@tb.jpeg", False),
    ("active-radio-90fm", "https://cdn.rlive.co.il/MFSINERJ5E955ZEB1IVQ92E19@tb.jpeg", False),
    ("ashams", "https://cdn.rlive.co.il/X2SUJQSGMU5FTXEXBX5NXHFJN@tb.png", False),
    ("avtoradio", "https://cdn.rlive.co.il/WQSB7RYYYIFT5CO77PKD6MUW9@tb.png", False),
    ("breslev-carmiel", "https://cdn.rlive.co.il/XZM5M5VPXRN42YF1ZKT2MJO19@tb.png", False),
    ("80sforever", "https://cdn.rlive.co.il/01JWDRYE01KGFP90J212ZS7KSN@tb.jpg", False),
    ("101fm", "https://cdn.rlive.co.il/K27CMC95ASXHDGYHYVV4PTZGH@tb.jpg", False),
    ("107-6", "https://cdn.rlive.co.il/QY9WERD2D8FF5YTN0TEBEG0FF@tb.jpg", False),
    ("100fm", "https://cdn.rlive.co.il/0XCID0SWGJPL9GVX4APRPZAG6@tb.jpg", False),
    ("917xfm", "https://cdn.rlive.co.il/01K5PCQHP4SDKGHKL2XLV6ZOE@tb.png", False),
    ("5radio", "https://cdn.rlive.co.il/E3CP0A3YWDHOL10X96GN13UYH@tb.jpeg", False),
    ("1062fm", "https://cdn.rlive.co.il/44Z5WG2G8I41CBQZCWS4PY0ON@tb.jpg", False),
    ("102fm", "https://cdn.rlive.co.il/01KDJAS3CBTT7EPH4GY79HXQ83@tb.jpg", False),
    ("88fm", "https://cdn.rlive.co.il/1EOCL2V7NH9CVDLIDM5XNUR5E@tb.png", False),
    ("antenne-salzburg", "https://cdn.rlive.co.il/MQJIC06CDATT4P6TKVI08E7ZD@tb.png", False),
    ("abc", "https://cdn.rlive.co.il/45VLJLU40424P3VB5FN5B4XKN@tb.png", False),
    ("radio-69fm", "https://cdn.rlive.co.il/59HLDOXTW8DHAK0SVZLX0E1YB@tb.png", False),
    ("breslev-carmiel", "https://cdn.rlive.co.il/XZM5M5VPXRN42YF1ZKT2MJO19@tb.png", False),
    ("80sforever", "https://cdn.rlive.co.il/01JWDRYE01KGFP90J212ZS7KSN@tb.jpg", False),
    ("101fm", "https://cdn.rlive.co.il/K27CMC95ASXHDGYHYVV4PTZGH@tb.jpg", False),
    ("107-6", "https://cdn.rlive.co.il/QY9WERD2D8FF5YTN0TEBEG0FF@tb.jpg", False),
    ("100fm", "https://cdn.rlive.co.il/0XCID0SWGJPL9GVX4APRPZAG6@tb.jpg", False),
    ("917xfm", "https://cdn.rlive.co.il/01K5PCQHP4SDKGHKL2XLV6ZOE@tb.png", False),
    ("5radio", "https://cdn.rlive.co.il/E3CP0A3YWDHOL10X96GN13UYH@tb.jpeg", False),
    ("1062fm", "https://cdn.rlive.co.il/44Z5WG2G8I41CBQZCWS4PY0ON@tb.jpg", False),
    ("dudi-fm", "https://cdn.rlive.co.il/BMNTLRRMO53O5EE4F08MC6BNV@tb.jpg", False),
    ("classical-king-fm", "https://cdn.rlive.co.il/ZI7WBRJ1QYS946TCTON45C2KE@tb.png", False),
    ("c14news", "https://cdn.rlive.co.il/01KDX2K39SAGPZA5Q0GY7B61V9@tb.jpg", False),
    ("bgu-radio", "https://cdn.rlive.co.il/2IO0I4FWLR1SHK85BNSM9WK9P@tb.jpg", False),
    ("capital-radio", "https://cdn.rlive.co.il/08RVHL3MPAWWN222AL3HU71K5@tb.png", False),
    ("classic-fm", "https://cdn.rlive.co.il/XE0GTU1AIK8HNHLK33H3Y46L6@tb.png", False),
    ("galey-israel", "https://cdn.rlive.co.il/01JX0S9Z94RW4PBGBJJPV8SRF1@tb.jpg", False),
    ("hamesh-99-5", "https://cdn.rlive.co.il/WBH9PL0V31IL8CXGMS45HD9YL@tb.png", False),
    ("glgltz", "https://cdn.rlive.co.il/OPQ52YPKRPO0SN3N3M463T4ZY@tb.jpeg", False),
    ("heart-london", "https://cdn.rlive.co.il/EDESZNLK9SHBRG1V92K8PA3MQ@tb.jpeg", False),
    ("israelivoiceradio", "https://cdn.rlive.co.il/GJKYN5O7MQB27N5ETFEKB55PJ@tb.jpg", False),
    ("harokdim", "https://cdn.rlive.co.il/PZ3NPQB52HF469UKY20M2RBHD@tb.png", False),
    ("i24news", "https://cdn.rlive.co.il/JQYMO1H5HLZ21UJ2OUDWHEWL5@tb.jpg", False),
    ("halachot", "https://cdn.rlive.co.il/01JHD4SN268PQZCJ851P3X35A1@tb.jpg", False),
    ("hatahana", "https://cdn.rlive.co.il/MKY9XI5N65P9Q931I4LHHYIR1@tb.jpeg", False),
    ("glz", "https://cdn.rlive.co.il/YFK1KYZ1TEHZGFHC3CTOU9A42@tb.png", False),
    ("itch-fm", "https://cdn.rlive.co.il/01K8RDWXV36BBC29K29B5QY02K@tb.jpg", False),
    ("jewish-music-stream", "https://cdn.rlive.co.il/PW167EE0USETH5NTOKBNE96AH@tb.jpg", False),
    ("efi-logue", "https://cdn.rlive.co.il/Y91GWYWYM8XY35GNZ3KHW8MDV@tb.png", False),
    ("carmelfm", "https://cdn.rlive.co.il/01JHCQQJT1NXD80QPJDX408B3H@tb.jpg", False),
    ("eilat-beach-radio", "https://cdn.rlive.co.il/2EX2WEK5MN08OYC2ETNEWIW22@tb.png", False),
    ("europa-fm-romania", "https://cdn.rlive.co.il/OLFENNUTH8K3P5T8CLBKUH1AB@tb.jpg", False),
    ("har-carmel", "https://cdn.rlive.co.il/01JQ92V2GAXGHFGW6D95YJDA3J@tb.jpg", False),
    ("franceinfo", "https://cdn.rlive.co.il/BRLY286X1WO7FBG8OTR1NMIWQ@tb.jpg", False),
    ("clube-fm", "https://cdn.rlive.co.il/124THOY6I0GQ6L86OODJPHZVZ@tb.png", False),
    ("israel-news-talk-radio", "https://cdn.rlive.co.il/Y094D36DEMP3KHKQI8EXKRDXR@tb.png", False),
    ("hitsil", "https://cdn.rlive.co.il/92Y7FVZKJ40NIX9XWVGNLK8LP@tb.jpeg", False),
    ("glglz-med", "https://cdn.rlive.co.il/SUKBTUBOS22HBDZ62VM5TB7AY@tb.png", False),
    ("joint", "https://cdn.rlive.co.il/5V0PGN5QYK38IR8YAVRM3JGKA@tb.png", False),
    ("joint-radio-beat", "https://cdn.rlive.co.il/BSLFVLLN8JB4LNY2WHZ3R8F78@tb.png", False),
    ("idobi", "https://cdn.rlive.co.il/DFXIZLRODMKI6H5BF4X4KZMF1@tb.png", False),
    ("hevrati", "https://cdn.rlive.co.il/01KA8BZJRRSK7F2EN2QXVQSSTP@tb.jpg", False),
    ("israeliradiomiami", "https://cdn.rlive.co.il/01KE4N243293WZ3PABGF759K15@tb.jpg", False),
    ("jazz24", "https://cdn.rlive.co.il/IZ8E7S1R1CBS6U79CQ9UPV5RL@tb.jpg", False),
    ("joint-radio-blues", "https://cdn.rlive.co.il/S8I8O13HIPHPTVOG295SBGX3O@tb.png", False),
    ("ivri6", "https://cdn.rlive.co.il/MJ67BQUY3RP1ZQ7DCY85IHMHU@tb.png", False),
    ("jewish-radio-kol-hai", "https://cdn.rlive.co.il/PJ06OR3WF8OBQWHUJ1PQU9SUR@tb.jpg", False),
    ("katzav-mizrahit", "https://cdn.rlive.co.il/O1QUR68N0L6VMR708TAA1AXNL@tb.jpg", False),
    ("kol-arim", "https://cdn.rlive.co.il/01JX057J28RE8S4G6VJ0NXJNSY@tb.jpg", False),
    ("kol-barama", "https://cdn.rlive.co.il/AR2TIEW680F2XN4FK78V8KJD7@tb.png", False),
    ("kol-dimona", "https://cdn.rlive.co.il/Q5W3SY68UK8PNO2DY0K28KC60@tb.jpg", False),
    ("klub-radio", "https://cdn.rlive.co.il/0VEQNFGIEVYEYPDB2I1DVXURD@tb.png", False),
    ("kol-hanegev", "https://cdn.rlive.co.il/VVQVKX1XSKBLP331PN1GZ19JX@tb.jpg", False),
    ("kol-hamusic", "https://cdn.rlive.co.il/YEK7QS9V00U6CL049V8Y1XDEV@tb.png", False),
    ("ketzev-yamtichoni", "https://cdn.rlive.co.il/A74DNTHK2ML37A5VF7HUS3OJW@tb.jpeg", False),
    ("kol-simcha-hits", "https://cdn.rlive.co.il/PZ8F8ZN2P5GWF762QWEJFGHF8@tb.jpg", False),
    ("knesset-channel", "https://cdn.rlive.co.il/JL80J86DTVVJ57EL58TA37NP9@tb.jpeg", False),
    ("kahol-yavan", "https://cdn.rlive.co.il/TQPC11E9LZAHABUFYCWM72AZH@tb.jpeg", False),
    ("kankids", "https://cdn.rlive.co.il/MP0WUEHFP1FBBFKHK7BINV21J@tb.jpg", False),
    ("kol-ramah", "https://cdn.rlive.co.il/5CQXXQ0J5897ZRTM47O81PLJV@tb.png", False),
    ("kol-galim", "https://cdn.rlive.co.il/9CZWB80A7QXR17Q6UMFYGZBLM@tb.jpeg", False),
    ("kol-halacha", "https://cdn.rlive.co.il/0PQONM1LWHXJQU6DJ4EGDOV7L@tb.png", False),
    ("kol-rishonim", "https://cdn.rlive.co.il/HTL8V6UFQWYH7GE2F1MHL2F4C@tb.jpeg", False),
    ("kol-ness-ziona", "https://cdn.rlive.co.il/TIFCZOSD23EA7AMU8CF2DAYV6@tb.jpg", False),
    ("kol-simcha-light", "https://cdn.rlive.co.il/0O9XZRXSRWU8GEUPH5OAH56R8@tb.jpg", False),
    ("kol-ramla", "https://cdn.rlive.co.il/G70P20872TZYQRVUQ5NR5128O@tb.png", False),
    ("kol-california", "https://cdn.rlive.co.il/0M8DVNTU1ZV1U9AAC9ZBC2ML7@tb.png", False),
    ("kol-hashfela", "https://cdn.rlive.co.il/HVBT67NQOO9FP7IFVH4AGGCWA@tb.jpeg", False),
    ("kol-hagolan", "https://cdn.rlive.co.il/T2JC2CCFSBMPIX5QCHBK126EU@tb.png", False),
    ("kol-hayam-haadom-102fm", "https://cdn.rlive.co.il/31LM500DP9LLECW3HQ904GRTL@tb.jpg", False),
    ("kan-11", "https://cdn.rlive.co.il/E2KIY0MDIQ0KQKST45NLET486@tb.jpg", False),
    ("kol-hayam-hatichon", "https://cdn.rlive.co.il/SBWD302YQPTN8A43HCXKUD0BH@tb.png", False),
    ("kol-ramat-hasharon-106fm", "https://cdn.rlive.co.il/08IVSFM2R0CPQQ2NTKI2RHALZ@tb.jpg", False),
    ("kol-israel-in-arabic", "https://cdn.rlive.co.il/SB1E0PMNXIEJ7I9N5ELBQW381@tb.png", False),
    ("lux-fm", "https://cdn.rlive.co.il/KVGZG6DMQ4MDJ17QW4L4EVSPY@tb.jpg", False),
    ("musayof", "https://cdn.rlive.co.il/8JMRSUSJH41QK0F5HIOCOSS7A@tb.jpg", False),
    ("love-songs-radio", "https://cdn.rlive.co.il/SZF3OBVGVUVW7LG7WOM69ANY7@tb.jpeg", False),
    ("malchut-pe-mehadrin-radio", "https://cdn.rlive.co.il/S1YWU0JV6IZ0BSMLUCIF3QUJL@tb.jpeg", False),
    ("kolhalev", "https://cdn.rlive.co.il/09L4M8X5T58RT0M9EEEZ6MXEF@tb.jpeg", False),
    ("metsiot", "https://cdn.rlive.co.il/H4EB3RW2U2W114R78M49MCH8U@tb.jpg", False),
    ("neurim-fm", "https://cdn.rlive.co.il/OWJVAVF7ZG4ZCL0TSCDGKPJIA@tb.jpg", False),
    ("mc-doualiya", "https://cdn.rlive.co.il/79ZNZAQO4HKBG5PAVGVNPWVH9@tb.jpeg", False),
    ("nas", "https://cdn.rlive.co.il/W31N1BFMV3KE8K92BS5J6JDTB@tb.png", False),
    ("kzradio", "https://cdn.rlive.co.il/FSU8OH34DTKUSYIDLDT21V68Z@tb.png", False),
    ("livetorani", "https://cdn.rlive.co.il/U2FUUUTJ3S3OK8XY34K8DMQH8@tb.jpeg", False),
    ("n12", "https://cdn.rlive.co.il/IN6BIO75F61GQCERXH0MIMSUP@tb.jpg", False),
    ("kolhaemet", "https://cdn.rlive.co.il/CRX8D6HLIBL5I1NZGRT8WN5A9@tb.jpeg", False),
    ("los40", "https://cdn.rlive.co.il/G74M15XJGQ4JS8MOW42L26G7H@tb.png", False),
    ("noshmim-mizrahit", "https://cdn.rlive.co.il/IPOXDD1LUW38URM2EEKQHAEH5@tb.png", False),
    ("pervoe-radio", "https://cdn.rlive.co.il/YF46NRDYRMI26T27HP3ZKHGXK@tb.jpeg", False),
    ("radio-99fm", "https://cdn.rlive.co.il/VJULPMZIBILYCPTFYWMV9VJAU@tb.png", False),
    ("nostalgia-963fm", "https://cdn.rlive.co.il/XUOIZRROA46LNR2O93I378B5P@tb.jpeg", False),
    ("radio-dikaun-mizrahi", "https://cdn.rlive.co.il/IXFGZNJOBZHRUWRPTOFY3E2SL@tb.jpg", False),
    ("radio-538", "https://cdn.rlive.co.il/TNDWYRYQY1LDNYDKEM91HFY7C@tb.png", False),
    ("radio-darom-9697fm", "https://cdn.rlive.co.il/01KE7BCWVT026PMY09T28M79JB@tb.jpg", False),
    ("radio-beat", "https://cdn.rlive.co.il/EPYJY69P4WZL7ZSQC2PUQNGI4@tb.png", False),
    ("radio-azori", "https://cdn.rlive.co.il/381T9FN5K61HDLUG0L03QC8QB@tb.jpg", False),
    ("radio-darom-1015fm", "https://cdn.rlive.co.il/01KE7BPNMX2NFZVMYMWPE5N85D@tb.jpg", False),
    ("open-university-radio", "https://cdn.rlive.co.il/2W353BJAKSZHMVV6FAPA8J5BS@tb.png", False),
    ("radio-2000", "https://cdn.rlive.co.il/MKQ3XATCSZZTBDPKKFQPBX36B@tb.jpg", False),
    ("radio-dante", "https://cdn.rlive.co.il/VEAP726S2BEEOCBRA3GOU3VGN@tb.png", False),
    ("radio-hidabroot", "https://cdn.rlive.co.il/3L1SJFS6A5NUE6UY7EJIY7BL1@tb.png", False),
    ("radio-haifa-107-5fm", "https://cdn.rlive.co.il/01K1667RMTN5B2HT0TH08VZK4N@tb.jpg", False),
    ("radio-eshel", "https://cdn.rlive.co.il/KP7EKSGIH0XM6A64G5P8UZGYR@tb.jpeg", False),
    ("radio-hamizrah", "https://cdn.rlive.co.il/AWB6IWAXVU99TGHW4AHPJIC6F@tb.jpg", False),
    ("radio-idan", "https://cdn.rlive.co.il/RMZBBX631TV7VSZQF710M87LZ@tb.jpg", False),
    ("radio-france-international", "https://cdn.rlive.co.il/SJO2CJP4QMLMOAG2GPU54ZIUL@tb.jpeg", False),
    ("radio-eol", "https://cdn.rlive.co.il/BV7TWAIQYPMQUMRWYJGM7U4EK@tb.jpeg", False),
    ("radio-breslev", "https://cdn.rlive.co.il/XRMBGSYEJVLA2RUA3BMSPXMQT@tb.png", False),
    ("lolfmradio", "https://cdn.rlive.co.il/1RY8N1SWUKWM3HV3ZP7FLYEO2@tb.png", False),
    ("levisrael", "https://cdn.rlive.co.il/GS0RUARE7L8559BMB8RGOSBZA@tb.jpeg", False),
    ("kolyaldeyisrael", "https://cdn.rlive.co.il/01JH834AD5QWJAHSEQG4DDB292@tb.jpg", False),
    ("metsiot-israeli", "https://cdn.rlive.co.il/WNQ1YNKMI3J3DP6NC15AQ1E6B@tb.jpg", False),
    ("radio-dvash-yevani", "https://cdn.rlive.co.il/066QTNP0RQQVARFSSZLXZXO7E@tb.jpg", False),
    ("radio-angelika", "https://cdn.rlive.co.il/01KGYEJB86EXW6S7SEYXPWSQPP@tb.jpg", False),
    ("radio-indie-israel", "https://cdn.rlive.co.il/3L3UGSOQVR5GZ6A3N1UG8RFGD@tb.jpg", False),
    ("martit", "https://cdn.rlive.co.il/QFGT2RDPAXIQUHIUC8V6Z9P7V@tb.png", False),
    ("musichevrati", "https://cdn.rlive.co.il/01KEXVTAHXX645GMSMNNTJZHS6@tb.jpg", False),
    ("radio-b-b-vostok", "https://cdn.rlive.co.il/KFFU87E22E3K7FT6JHZAW10NK@tb.png", False),
    ("radio-breslev-kol-hanahal", "https://cdn.rlive.co.il/89LPALY5JDRT5K6YHBAGYVVPB@tb.png", False),
    ("radio-idan-dati", "https://cdn.rlive.co.il/RH5RY2T2VIDLYIJM3C9ZQ59ZE@tb.jpg", False),
    ("radio-kahol-lavan", "https://cdn.rlive.co.il/VIIACHOJX815Z355CD8S44NO0@tb.jpg", False),
    ("radio-kol-hatzafon", "https://cdn.rlive.co.il/3P8YG0X3H6GOGHJMB21Y5CF46@tb.png", False),
    ("radio-mevaser-tov", "https://cdn.rlive.co.il/WFCTGQE6SI8LDYNBNKLU9GZFD@tb.png", False),
    ("radio-nova", "https://cdn.rlive.co.il/3UCVC62F86LFVC0AIFW9LD489@tb.png", False),
    ("radio-mozart", "https://cdn.rlive.co.il/01JW6ZNB5MWH4SEVZE42W8JGYV@tb.jpg", False),
    ("radio-mania", "https://cdn.rlive.co.il/79ZSQXRB8MC7CXB0J10VTABOS@tb.png", False),
    ("radio-savta", "https://cdn.rlive.co.il/S2M90SJB6MEFIUBP6ZHZL8CCP@tb.jpeg", False),
    ("radio-ran", "https://cdn.rlive.co.il/YBR3K1LZ81BQYLTKRER8BMKHU@tb.png", False),
    ("radio-kesem", "https://cdn.rlive.co.il/U9O576FII5FP31BT47753XNN6@tb.jpg", False),
    ("radio-sahar", "https://cdn.rlive.co.il/Q7VXBWQSN7F3JKGL6GBXQY5C2@tb.png", False),
    ("radio-krayot-fm", "https://cdn.rlive.co.il/SQRUYBDGQNSRQBY2TN4QMPHBC@tb.jpg", False),
    ("radio-kol-simcha", "https://cdn.rlive.co.il/YPRRYKILUPMM6ME4ATT4EY99Z@tb.png", False),
    ("radio-inn-7", "https://cdn.rlive.co.il/1PHV5SAXLFSFVS4FEJNIEWXHL@tb.jpg", False),
    ("radio-nahariya", "https://cdn.rlive.co.il/XPXTIU767CPPPI11YGGZHQ1LE@tb.jpg", False),
    ("radio-kol-hakinneret", "https://cdn.rlive.co.il/ZPS8CYZG2N400H57WS8DAO6NX@tb.jpeg", False),
    ("radio-nina", "https://cdn.rlive.co.il/1YX404DDRIULQOKZHQXB7A0GZ@tb.png", False),
    ("radio-shabazi", "https://cdn.rlive.co.il/SQKAYROCUAU7K4MZYEKEJ7HFR@tb.png", False),
    ("radio5", "https://cdn.rlive.co.il/01KE7C0GNH9WM4HK453TWPECWT@tb.jpg", False),
    ("rai-radio-1", "https://cdn.rlive.co.il/5MMBB9CTC5QFYAF4KT5J53HIA@tb.jpg", False),
    ("radio-one", "https://cdn.rlive.co.il/4V6IRD7F17HGI5O7Q3KEFD8K4@tb.png", False),
    ("radio-menta", "https://cdn.rlive.co.il/BW067A4S6IK6HTU6UD3UPR7ID@tb.png", False),
    ("radio-kol-netanya", "https://cdn.rlive.co.il/0O60YJXL52GT2HJDKUWB66BSL@tb.jpeg", False),
    ("radio-sol", "https://cdn.rlive.co.il/01KE4KSW7DYB629HYWKW0GC43T@tb.jpg", False),
    ("radio106", "https://cdn.rlive.co.il/O0KC4OM07G3ZD9XZB0WHCS6AN@tb.png", False),
    ("reka", "https://cdn.rlive.co.il/3R2X8LNY02RNO7160SPOKBUZV@tb.png", False),
    ("reshet-gimmel", "https://cdn.rlive.co.il/7UYWVYTP1OMQXSDXMK81X4HO9@tb.png", False),
    ("radio-veronika", "https://cdn.rlive.co.il/01K86DD0S24AD8QTXTMHZRSY5V@tb.jpg", False),
    ("radio-kol-izrael", "https://cdn.rlive.co.il/LUCQ4SGN5QVGYZG1R7UOAM93R@tb.jpg", False),
    ("radiofix2525", "https://cdn.rlive.co.il/3IPEZ1FSFD527V8SDJ4RI2MDV@tb.png", False),
    ("radio-yasoo", "https://cdn.rlive.co.il/X817C9MVWEUSDVIR5CO448KEG@tb.jpeg", False),
    ("radio-sawa", "https://cdn.rlive.co.il/JOSRJ19XZ2MGJQZ8YH2AYA40Z@tb.jpeg", False),
    ("reshet-moreshet", "https://cdn.rlive.co.il/SPG1KPG6QN0XMB9YNRTDYA3NY@tb.png", False),
    ("reshet-bet", "https://cdn.rlive.co.il/01KJQRYEBPDQNP8W2Y1938QTH0@tb.jpg", False),
    ("reshet-aleph", "https://cdn.rlive.co.il/FW9ZX8YJIXMACQB6XJERA4UBM@tb.png", False),
    ("radioplus", "https://cdn.rlive.co.il/YPXMA31A9E75WQB03LQFD1TUN@tb.png", False),
    ("salsa-tel-aviv", "https://cdn.rlive.co.il/AJV9XNNE94VF145NI0IJFCLRY@tb.jpg", False),
    ("zerock", "https://cdn.rlive.co.il/0NGCGB0A6G94678XUNB0SVZBA@tb.jpg", False),
    ("toker-fm", "https://cdn.rlive.co.il/UELAKGHAKA3HJC7T7R8Y82H4H@tb.png", False),
    ("streetstune-radio", "https://cdn.rlive.co.il/L2ZA9KOLX4AKNNG90KKSTH875@tb.jpeg", False),
    ("shorts", "https://cdn.rlive.co.il/01JHD5DHJMEDKC451AJ1G756M9@tb.jpg", False),
    ("zlilei-hakrayot", "https://cdn.rlive.co.il/YQ46EJ1GVBZLOA7F8Y2ML0NKG@tb.jpg", False),
    ("voice-of-america", "https://cdn.rlive.co.il/QUZC3HGOEPOGN8GCYB1KC45RW@tb.png", False),
    ("teder", "https://cdn.rlive.co.il/16GLLT44H25UX49IO43R2607W@tb.jpeg", False),
    ("the-best", "https://cdn.rlive.co.il/01K071JA3FNF3ZYDX1RG48D7ZE@tb.jpg", False),
    ("smooth-70s", "https://cdn.rlive.co.il/FWOVAVBQCJ5G127R0V6896M2A@tb.jpg", False),
    ("sumba887", "https://cdn.rlive.co.il/GBFEU9HUKA0RV8C7WPN35KHJV@tb.jpg", False),
    ("salseo-radio", "https://cdn.rlive.co.il/L3P11I6178S4W7LK0PVM443UO@tb.png", False),
    ("up2dance", "https://cdn.rlive.co.il/VPCVA8R1YJ9X4THLQV8NHA89F@tb.jpeg", False),
    ("teen-buzz", "https://cdn.rlive.co.il/2NJF5XP5BMNAQEKW3KHKEPH1S@tb.png", False),
    # ── ערוצי קול חי מיוזיק ──
    ("kcm-10", "https://images.kcm.fm/channelImage/kcm-10.jpg", True),
    ("kcm-106", "https://images.kcm.fm/channelImage/kcm-106.jpg", True),
    ("kcm-107", "https://images.kcm.fm/channelImage/kcm-107.jpg", True),
    ("kcm-11", "https://images.kcm.fm/channelImage/kcm-11.jpg", True),
    ("kcm-112", "https://images.kcm.fm/channelImage/kcm-112.jpg", True),
    ("kcm-113", "https://images.kcm.fm/channelImage/kcm-113.jpg", True),
    ("kcm-114", "https://images.kcm.fm/channelImage/kcm-114.jpg", True),
    ("kcm-12", "https://images.kcm.fm/channelImage/kcm-12.jpg", True),
    ("kcm-13", "https://images.kcm.fm/channelImage/kcm-13.jpg", True),
    ("kcm-15", "https://images.kcm.fm/channelImage/kcm-15.jpg", True),
    ("kcm-16", "https://images.kcm.fm/channelImage/kcm-16.jpg", True),
    ("kcm-17", "https://images.kcm.fm/channelImage/kcm-17.jpg", True),
    ("kcm-19", "https://images.kcm.fm/channelImage/kcm-19.jpg", True),
    ("kcm-20", "https://images.kcm.fm/channelImage/kcm-20.jpg", True),
    ("kcm-21", "https://images.kcm.fm/channelImage/kcm-21.jpg", True),
    ("kcm-22", "https://images.kcm.fm/channelImage/kcm-22.jpg", True),
    ("kcm-23", "https://images.kcm.fm/channelImage/kcm-23.jpg", True),
    ("kcm-25", "https://images.kcm.fm/channelImage/kcm-25.jpg", True),
    ("kcm-26", "https://images.kcm.fm/channelImage/kcm-26.jpg", True),
    ("kcm-27", "https://images.kcm.fm/channelImage/kcm-27.jpg", True),
    ("kcm-28", "https://images.kcm.fm/channelImage/kcm-28.jpg", True),
    ("kcm-29", "https://images.kcm.fm/channelImage/kcm-29.jpg", True),
    ("kcm-3", "https://images.kcm.fm/channelImage/kcm-3.jpg", True),
    ("kcm-30", "https://images.kcm.fm/channelImage/kcm-30.jpg", True),
    ("kcm-31", "https://images.kcm.fm/channelImage/kcm-31.jpg", True),
    ("kcm-32", "https://images.kcm.fm/channelImage/kcm-32.jpg", True),
    ("kcm-33", "https://images.kcm.fm/channelImage/kcm-33.jpg", True),
    ("kcm-34", "https://images.kcm.fm/channelImage/kcm-34.jpg", True),
    ("kcm-35", "https://images.kcm.fm/channelImage/kcm-35.jpg", True),
    ("kcm-39", "https://images.kcm.fm/channelImage/kcm-39.jpg", True),
    ("kcm-4", "https://images.kcm.fm/channelImage/kcm-4.jpg", True),
    ("kcm-40", "https://images.kcm.fm/channelImage/kcm-40.jpg", True),
    ("kcm-41", "https://images.kcm.fm/channelImage/kcm-41.jpg", True),
    ("kcm-42", "https://images.kcm.fm/channelImage/kcm-42.jpg", True),
    ("kcm-46", "https://images.kcm.fm/channelImage/kcm-46.jpg", True),
    ("kcm-48", "https://images.kcm.fm/channelImage/kcm-48.jpg", True),
    ("kcm-49", "https://images.kcm.fm/channelImage/kcm-49.jpg", True),
    ("kcm-5", "https://images.kcm.fm/channelImage/kcm-5.jpg", True),
    ("kcm-51", "https://images.kcm.fm/channelImage/kcm-51.jpg", True),
    ("kcm-52", "https://images.kcm.fm/channelImage/kcm-52.jpg", True),
    ("kcm-53", "https://images.kcm.fm/channelImage/kcm-53.jpg", True),
    ("kcm-54", "https://images.kcm.fm/channelImage/kcm-54.jpg", True),
    ("kcm-55", "https://images.kcm.fm/channelImage/kcm-55.jpg", True),
    ("kcm-56", "https://images.kcm.fm/channelImage/kcm-56.jpg", True),
    ("kcm-57", "https://images.kcm.fm/channelImage/kcm-57.jpg", True),
    ("kcm-58", "https://images.kcm.fm/channelImage/kcm-58.jpg", True),
    ("kcm-59", "https://images.kcm.fm/channelImage/kcm-59.jpg", True),
    ("kcm-6", "https://images.kcm.fm/channelImage/kcm-6.jpg", True),
    ("kcm-60", "https://images.kcm.fm/channelImage/kcm-60.jpg", True),
    ("kcm-61", "https://images.kcm.fm/channelImage/kcm-61.jpg", True),
    ("kcm-62", "https://images.kcm.fm/channelImage/kcm-62.jpg", True),
    ("kcm-63", "https://images.kcm.fm/channelImage/kcm-63.jpg", True),
    ("kcm-65", "https://images.kcm.fm/channelImage/kcm-65.jpg", True),
    ("kcm-66", "https://images.kcm.fm/channelImage/kcm-66.jpg", True),
    ("kcm-67", "https://images.kcm.fm/channelImage/kcm-67.jpg", True),
    ("kcm-68", "https://images.kcm.fm/channelImage/kcm-68.jpg", True),
    ("kcm-69", "https://images.kcm.fm/channelImage/kcm-69.jpg", True),
    ("kcm-7", "https://images.kcm.fm/channelImage/kcm-7.jpg", True),
    ("kcm-70", "https://images.kcm.fm/channelImage/kcm-70.jpg", True),
    ("kcm-72", "https://images.kcm.fm/channelImage/kcm-72.jpg", True),
    ("kcm-73", "https://images.kcm.fm/channelImage/kcm-73.jpg", True),
    ("kcm-74", "https://images.kcm.fm/channelImage/kcm-74.jpg", True),
    ("kcm-75", "https://images.kcm.fm/channelImage/kcm-75.jpg", True),
    ("kcm-76", "https://images.kcm.fm/channelImage/kcm-76.jpg", True),
    ("kcm-77", "https://images.kcm.fm/channelImage/kcm-77.jpg", True),
    ("kcm-78", "https://images.kcm.fm/channelImage/kcm-78.jpg", True),
    ("kcm-79", "https://images.kcm.fm/channelImage/kcm-79.jpg", True),
    ("kcm-8", "https://images.kcm.fm/channelImage/kcm-8.jpg", True),
    ("kcm-80", "https://images.kcm.fm/channelImage/kcm-80.jpg", True),
    ("kcm-82", "https://images.kcm.fm/channelImage/kcm-82.jpg", True),
    ("kcm-85", "https://images.kcm.fm/channelImage/kcm-85.jpg", True),
    ("kcm-9", "https://images.kcm.fm/channelImage/kcm-9.jpg", True),
]

ok = err = skip = 0
for i, (sid, url, is_kcm) in enumerate(ALL_LOGOS):
    result = download(sid, url, is_kcm)
    if result:
        ok += 1
    else:
        err += 1
        print(f"  ❌ {sid}")
    if (i+1) % 20 == 0:
        print(f"  ... {i+1}/{len(ALL_LOGOS)} ({ok} ✅)")
    time.sleep(0.05)  # נימוס לשרת

print(f"\n✅ סיום! הצלחות: {ok}/{len(ALL_LOGOS)}  |  שגיאות: {err}")
print(f"הלוגואים נשמרו ב: {os.path.abspath(ASSETS_DIR)}")



