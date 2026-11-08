import type { CSSProperties, ReactNode } from "react";

// The drawings of docs/PLAN_V7.md, decision 4: flat shapes in the four colours, outlined in olive.
// Decoration only, aria-hidden: the text beside each says what it shows. A jar is drawn to stand
// on a shelf - its foot on the bottom of its 120 x 150 box.

const CREAM = "#f5efe3";
const OLIVE = "#4f5b2a";
const OCHRE = "#b8892d";
const TAN = "#d8c9a8";

type JarProps = { className?: string; style?: CSSProperties };

function Jar({ className, style, children }: JarProps & { children: ReactNode }) {
  return (
    <svg viewBox="0 0 120 150" aria-hidden="true" className={className} style={style}>
      {children}
    </svg>
  );
}

/** Recipes: a tall jar with a clamp lid, full of preserves. */
export function RecipesJar(props: JarProps) {
  return (
    <Jar {...props}>
      <rect x="20" y="30" width="80" height="118" rx="14" fill={CREAM} stroke={OLIVE} strokeWidth="2.5" />
      <rect x="25" y="58" width="70" height="86" rx="10" fill={OCHRE} />
      <rect x="30" y="82" width="60" height="34" rx="3" fill={CREAM} stroke={OLIVE} strokeWidth="2" />
      <path d="M40 94h40M44 104h32" stroke={OCHRE} strokeWidth="2.5" strokeLinecap="round" />
      <rect x="28" y="8" width="64" height="16" rx="4" fill={OLIVE} />
      <path d="M34 24v12M86 24v12M34 36h52" stroke={OLIVE} strokeWidth="2.5" fill="none" strokeLinecap="round" />
    </Jar>
  );
}

/** Categories: a wide jar of three layers. */
export function CategoriesJar(props: JarProps) {
  return (
    <Jar {...props}>
      <defs>
        <clipPath id="categories-jar">
          <rect x="11" y="61" width="98" height="86" rx="15" />
        </clipPath>
      </defs>
      <rect x="10" y="60" width="100" height="88" rx="16" fill={CREAM} />
      <g clipPath="url(#categories-jar)">
        <rect x="10" y="120" width="100" height="30" fill={OLIVE} />
        <rect x="10" y="98" width="100" height="22" fill={OCHRE} />
        <rect x="10" y="80" width="100" height="18" fill={TAN} />
      </g>
      <rect x="10" y="60" width="100" height="88" rx="16" fill="none" stroke={OLIVE} strokeWidth="2.5" />
      <rect x="16" y="46" width="88" height="16" rx="4" fill={OLIVE} />
    </Jar>
  );
}

/** Ingredients: a round jar of grains under a cloth tied with twine. */
export function IngredientsJar(props: JarProps) {
  const grains: [number, number][] = [
    [40, 120], [54, 128], [68, 118], [82, 126], [48, 106], [62, 102], [76, 108], [36, 134], [84, 138], [60, 138],
  ];
  const seeds: [number, number][] = [[56, 114], [72, 132], [44, 124], [88, 114]];
  return (
    <Jar {...props}>
      <rect x="16" y="46" width="88" height="102" rx="40" fill={CREAM} stroke={OLIVE} strokeWidth="2.5" />
      {grains.map(([cx, cy]) => (
        <circle key={`${cx}-${cy}`} cx={cx} cy={cy} r="4" fill={OCHRE} />
      ))}
      {seeds.map(([cx, cy]) => (
        <circle key={`${cx}-${cy}`} cx={cx} cy={cy} r="3" fill={OLIVE} />
      ))}
      <path
        d="M14 58 Q60 22 106 58 L110 70 Q60 56 10 70 Z"
        fill={TAN}
        stroke={OLIVE}
        strokeWidth="2.5"
        strokeLinejoin="round"
      />
      <path d="M16 64 Q60 52 104 64" stroke={OCHRE} strokeWidth="3.5" fill="none" strokeLinecap="round" />
    </Jar>
  );
}

/** Units: a measuring jug with marks. */
export function UnitsJug(props: JarProps) {
  return (
    <Jar {...props}>
      <path d="M23.6 100 L96.4 100 L100 148 L20 148 Z" fill={TAN} />
      <path d="M28 40 L92 40 L100 148 L20 148 Z" fill="none" stroke={OLIVE} strokeWidth="2.5" strokeLinejoin="round" />
      <path d="M28 40 L16 30" stroke={OLIVE} strokeWidth="2.5" strokeLinecap="round" />
      <path d="M94 58 C118 62 118 110 98 118" stroke={OLIVE} strokeWidth="6" fill="none" strokeLinecap="round" />
      <path d="M32 64h14M31 82h10M30 100h14M28 118h10M27 136h14" stroke={OLIVE} strokeWidth="2.5" strokeLinecap="round" />
    </Jar>
  );
}

/** Tags: a jar with a tag on a string. */
export function TagsJar(props: JarProps) {
  return (
    <Jar {...props}>
      <rect x="22" y="44" width="70" height="104" rx="12" fill={CREAM} stroke={OLIVE} strokeWidth="2.5" />
      <rect x="27" y="82" width="60" height="62" rx="8" fill={OLIVE} />
      <rect x="28" y="32" width="58" height="14" rx="4" fill={OLIVE} />
      <path d="M84 46 Q104 54 102 78" stroke={OCHRE} strokeWidth="2.5" fill="none" strokeLinecap="round" />
      <path
        d="M94 78 L108 78 L115 86 L115 112 L94 112 Z"
        fill={TAN}
        stroke={OLIVE}
        strokeWidth="2.5"
        strokeLinejoin="round"
      />
      <circle cx="104" cy="86" r="2.8" fill={CREAM} stroke={OLIVE} strokeWidth="1.5" />
    </Jar>
  );
}

/** A page not found: the recipes jar, standing empty. */
export function EmptyJar(props: JarProps) {
  return (
    <Jar {...props}>
      <rect x="20" y="30" width="80" height="118" rx="14" fill={CREAM} stroke={OLIVE} strokeWidth="2.5" />
      <path d="M30 136h60" stroke={TAN} strokeWidth="5" strokeLinecap="round" />
      <rect x="28" y="8" width="64" height="16" rx="4" fill={OLIVE} />
      <path d="M34 24v12M86 24v12M34 36h52" stroke={OLIVE} strokeWidth="2.5" fill="none" strokeLinecap="round" />
    </Jar>
  );
}

/** Above the sign-in pages: an ear of wheat and a dill sprig, crossed at their stalks. */
export function WheatAndDill({ className }: { className?: string }) {
  // Each plant is drawn upright from its foot at (0, 0), then turned: the wheat leans left, the
  // dill right, and their stalks cross near the bottom.
  const grainHeights = [-44, -54, -64, -74];
  const rays = [-62, -36, -12, 12, 36, 62].map((degrees) => {
    const radians = (degrees * Math.PI) / 180;
    return { x: 22 * Math.sin(radians), y: -80 - 22 * Math.cos(radians) };
  });
  return (
    <svg viewBox="20 0 140 120" aria-hidden="true" className={className}>
      <g transform="translate(100 114) rotate(-20)" stroke={OLIVE} strokeLinecap="round">
        <path d="M0 0 Q2 -40 0 -86" strokeWidth="2.5" fill="none" />
        {grainHeights.map((y) => (
          <g key={y} strokeWidth="1.5">
            <path d={`M-7 ${y - 6} L-15 ${y - 26}`} />
            <path d={`M7 ${y - 6} L15 ${y - 26}`} />
            <ellipse cx="-5" cy={y} rx="4.5" ry="8" transform={`rotate(-22 -5 ${y})`} fill={OCHRE} strokeWidth="2" />
            <ellipse cx="5" cy={y} rx="4.5" ry="8" transform={`rotate(22 5 ${y})`} fill={OCHRE} strokeWidth="2" />
          </g>
        ))}
        <path d="M0 -92 L0 -108" strokeWidth="1.5" />
        <ellipse cx="0" cy="-86" rx="4.5" ry="8" fill={OCHRE} strokeWidth="2" />
      </g>
      <g transform="translate(66 114) rotate(28)" stroke={OLIVE} strokeLinecap="round" fill="none">
        <path d="M0 0 Q-3 -40 0 -80" strokeWidth="2.5" />
        {/* A feathered leaf on each side. */}
        <path d="M-1 -34 Q12 -44 20 -60 M5 -39 l7 1 M10 -45 l7 0 M15 -52 l6 -1" strokeWidth="1.6" />
        <path d="M-1 -20 Q-14 -28 -22 -42 M-7 -24 l-7 2 M-12 -30 l-7 1 M-17 -36 l-6 0" strokeWidth="1.6" />
        {rays.map(({ x, y }) => (
          <g key={x}>
            <path d={`M0 -80 L${x} ${y}`} strokeWidth="1.5" />
            <circle cx={x} cy={y} r="3.4" fill={OCHRE} strokeWidth="1.5" />
          </g>
        ))}
      </g>
    </svg>
  );
}
