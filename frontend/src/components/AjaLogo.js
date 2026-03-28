import React from "react";

/**
 * لوگوی کتابخانه هوشمند آجا
 * الهام‌گرفته از نشان ارتش جمهوری اسلامی ایران
 * با جایگزینی لنگر با کتاب باز (نماد کتابخانه)
 */
const AjaLogo = ({ size = 56, className = "" }) => {
  const cx = 100, cy = 112; // مرکز نشان

  // ── تولید برگ‌های گلدار (Wreath) ──
  const makeLeaf = (angleDeg, r, key) => {
    const rad = (angleDeg * Math.PI) / 180;
    const x = cx + r * Math.cos(rad);
    const y = cy + r * Math.sin(rad);
    return (
      <ellipse
        key={key}
        cx={x} cy={y}
        rx={2.8} ry={9.5}
        fill="#C8A04C"
        transform={`rotate(${angleDeg - 90},${x},${y})`}
      />
    );
  };

  const leafCount = 13;
  const leftLeaves = [], rightLeaves = [];
  for (let i = 0; i < leafCount; i++) {
    const t = i / (leafCount - 1);
    // شاخه چپ: 120° (پایین‌چپ) تا 240° (بالا‌چپ)
    const la = 120 + t * 120;
    leftLeaves.push(makeLeaf(la, 77 + (i % 2 === 0 ? 0 : 6), `l${i}`));
    // شاخه راست: 60° (پایین‌راست) تا -60° (بالا‌راست)
    const ra = 60 - t * 120;
    rightLeaves.push(makeLeaf(ra, 77 + (i % 2 === 0 ? 0 : 6), `r${i}`));
  }

  return (
    <svg
      width={size}
      height={Math.round(size * 1.1)}
      viewBox="0 0 200 220"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
    >
      {/* ══════════════ گلدان (Wreath) ══════════════ */}
      {leftLeaves}
      {rightLeaves}

      {/* دانه‌های پایین گلدان */}
      <circle cx="87"  cy="192" r="3"   fill="#C8A04C" />
      <circle cx="94"  cy="194" r="3"   fill="#C8A04C" />
      <circle cx="100" cy="195" r="3.5" fill="#C8A04C" />
      <circle cx="106" cy="194" r="3"   fill="#C8A04C" />
      <circle cx="113" cy="192" r="3"   fill="#C8A04C" />
      <path d="M87,192 Q100,199 113,192" fill="none" stroke="#C8A04C" strokeWidth="1.5"/>

      {/* ══════════════ دایره اصلی (مشکی) ══════════════ */}
      <circle cx={cx} cy={cy} r="70" fill="#070c13" stroke="#C8A04C" strokeWidth="3"/>
      {/* حلقه تزئینی داخلی */}
      <circle cx={cx} cy={cy} r="63" fill="none" stroke="#C8A04C"
        strokeWidth="0.7" strokeDasharray="4 3" opacity="0.45"/>

      {/* ══════════════ دایره سبز مرکزی ══════════════ */}
      <circle cx={cx} cy={cy} r="55" fill="#1b6230"/>
      <circle cx={cx} cy={cy} r="55" fill="none" stroke="#C8A04C" strokeWidth="1.2" opacity="0.65"/>
      <circle cx={cx} cy={cy} r="47" fill="none" stroke="#C8A04C" strokeWidth="0.5" opacity="0.25"/>

      {/* متن دایره‌ای (موتو) */}
      <defs>
        <path id="mottoPath"
          d={`M${cx},${cy - 50} A50,50 0 1 0 ${cx + 0.01},${cy - 50}`}/>
      </defs>
      <text fontSize="6" fill="#C8A04C" fontFamily="sans-serif">
        <textPath href="#mottoPath" startOffset="5%">
          وَإِنَّ جُندَنَا لَهُمُ الْغَالِبُونَ
        </textPath>
      </text>

      {/* ══════════════ بال عقاب ══════════════ */}
      {/* بال چپ */}
      <path
        d="M 96,109
           C 81,102 64,95 44,93
           L 47,101
           C 62,99 77,105 94,113
           Z"
        fill="#C8A04C"
      />
      {/* پر بیرونی بال چپ */}
      <path d="M44,93 L40,101 L48,101" fill="#C8A04C" stroke="#9A7A28" strokeWidth="0.7"/>
      {/* هایلایت بال چپ */}
      <path d="M96,109 C84,104 70,98 52,96"
        fill="none" stroke="#E8C96C" strokeWidth="0.9" opacity="0.55"/>

      {/* بال راست (آینه بال چپ) */}
      <path
        d="M 104,109
           C 119,102 136,95 156,93
           L 153,101
           C 138,99 123,105 106,113
           Z"
        fill="#C8A04C"
      />
      {/* پر بیرونی بال راست */}
      <path d="M156,93 L160,101 L152,101" fill="#C8A04C" stroke="#9A7A28" strokeWidth="0.7"/>
      {/* هایلایت بال راست */}
      <path d="M104,109 C116,104 130,98 148,96"
        fill="none" stroke="#E8C96C" strokeWidth="0.9" opacity="0.55"/>

      {/* ══════════════ بدن عقاب ══════════════ */}
      <path
        d="M100,94
           C 109,97 112,104 109,116
           L 100,120
           L  91,116
           C  88,104  91,97 100,94
           Z"
        fill="#C8A04C"
      />

      {/* ══════════════ سر عقاب ══════════════ */}
      <circle cx="100" cy="93" r="8.5" fill="#C8A04C"/>
      {/* منقار */}
      <path d="M107,92 L114,95 L108,98Z" fill="#8B6914"/>
      {/* چشم */}
      <circle cx="104" cy="90" r="2" fill="#070c13"/>
      <circle cx="104.5" cy="89.5" r="0.8" fill="rgba(255,255,255,0.5)"/>

      {/* ══════════════ کتاب باز ══════════════ */}
      {/* جلد چپ */}
      <path d="M74,120 L99,117 L99,137 L74,140Z" fill="#E8C96C"/>
      {/* جلد راست */}
      <path d="M126,120 L101,117 L101,137 L126,140Z" fill="#C8A04C"/>
      {/* خط وسط (ستون) */}
      <line x1="100" y1="117" x2="100" y2="137" stroke="#070c13" strokeWidth="1.6"/>
      {/* خطوط صفحات – چپ */}
      <line x1="79"  y1="123.5" x2="98" y2="122"   stroke="#070c13" strokeWidth="0.75" opacity="0.5"/>
      <line x1="79"  y1="127.5" x2="98" y2="126"   stroke="#070c13" strokeWidth="0.75" opacity="0.5"/>
      <line x1="79"  y1="131.5" x2="98" y2="130"   stroke="#070c13" strokeWidth="0.75" opacity="0.5"/>
      {/* خطوط صفحات – راست */}
      <line x1="121" y1="123.5" x2="102" y2="122"  stroke="#070c13" strokeWidth="0.75" opacity="0.4"/>
      <line x1="121" y1="127.5" x2="102" y2="126"  stroke="#070c13" strokeWidth="0.75" opacity="0.4"/>
      <line x1="121" y1="131.5" x2="102" y2="130"  stroke="#070c13" strokeWidth="0.75" opacity="0.4"/>
      {/* قوس پایین کتاب */}
      <path d="M74,140 Q100,146 126,140" fill="none" stroke="#9A7A28" strokeWidth="1.2"/>

      {/* ══════════════ شمشیرهای متقاطع ══════════════ */}
      {/* شمشیر چپ */}
      <line x1="62" y1="162" x2="89"  y2="143" stroke="#C8A04C" strokeWidth="2.2" strokeLinecap="round"/>
      <ellipse cx="62" cy="162" rx="5.5" ry="2" fill="#C8A04C" transform="rotate(-55,62,162)"/>
      <circle cx="59" cy="164" r="2" fill="#C8A04C"/>
      {/* شمشیر راست */}
      <line x1="138" y1="162" x2="111" y2="143" stroke="#C8A04C" strokeWidth="2.2" strokeLinecap="round"/>
      <ellipse cx="138" cy="162" rx="5.5" ry="2" fill="#C8A04C" transform="rotate(55,138,162)"/>
      <circle cx="141" cy="164" r="2" fill="#C8A04C"/>

      {/* ══════════════ دایره نشان بالایی ══════════════ */}
      {/* رابط بین دایره بالایی و نشان اصلی */}
      <rect x="92" y="40" width="16" height="9" fill="#070c13"/>

      {/* دایره بالایی */}
      <circle cx="100" cy="29" r="22" fill="#070c13" stroke="#C8A04C" strokeWidth="2.8"/>

      {/* نماد ساده‌شده (الهام از نشان ملی ایران) */}
      {/* ستون مرکزی */}
      <line x1="100" y1="12" x2="100" y2="47" stroke="#C8A04C" strokeWidth="2.2" strokeLinecap="round"/>
      {/* گلبرگ چپ (لاله) */}
      <path d="M100,20 C90,17 86,23 89,29 C91,34 98,32 100,27"
        stroke="#C8A04C" strokeWidth="1.7" fill="none" strokeLinecap="round"/>
      {/* گلبرگ راست (لاله) */}
      <path d="M100,20 C110,17 114,23 111,29 C109,34 102,32 100,27"
        stroke="#C8A04C" strokeWidth="1.7" fill="none" strokeLinecap="round"/>
      {/* میله افقی */}
      <line x1="91" y1="37" x2="109" y2="37" stroke="#C8A04C" strokeWidth="1.6" strokeLinecap="round"/>
      {/* گوی بالا */}
      <circle cx="100" cy="11" r="3" fill="#C8A04C"/>

      {/* ══════════════ نوار پایینی ══════════════ */}
      <path
        d="M31,177 C51,173 149,173 169,177
           L169,194 C149,198 51,198 31,194Z"
        fill="#1b6230"
      />
      {/* پیچ چپ نوار */}
      <path d="M31,177 L18,183 L31,194" fill="#145228"/>
      {/* پیچ راست نوار */}
      <path d="M169,177 L182,183 L169,194" fill="#145228"/>
      {/* حاشیه نوار */}
      <path
        d="M31,177 C51,173 149,173 169,177 L169,194 C149,198 51,198 31,194Z"
        fill="none" stroke="#C8A04C" strokeWidth="0.9" opacity="0.55"
      />
      {/* متن نوار */}
      <text
        x="100" y="189"
        textAnchor="middle"
        fontSize="8.5"
        fontWeight="bold"
        fill="#C8A04C"
        fontFamily="Vazirmatn,'B Nazanin',Arial,sans-serif"
        dominantBaseline="middle"
      >
        ارتش جمهوری اسلامی ایران
      </text>
    </svg>
  );
};

export default AjaLogo;
