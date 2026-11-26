import React, { useEffect, useRef, useState } from "react";

const formatTicker = [
  "PNG",
  "JPG",
  "WEBP",
  "HEIC",
  "AVIF",
  "GIF",
  "TIFF",
  "Сжатие",
  "Пакетная обработка",
  "Приватно",
];

const features = [
  {
    icon: "formats",
    color: "var(--accent)",
    title: "7 форматов изображений",
    text: "Выбирайте PNG, JPG, HEIC, AVIF, WEBP, GIF или TIFF для подходящей задачи.",
  },
  {
    icon: "quality",
    color: "var(--accent)",
    title: "Точная настройка сжатия",
    text: "Настраивайте степень сжатия ползунком от 0 до 100% и подбирайте баланс качества и размера.",
  },
  {
    icon: "batch",
    color: "var(--accent)",
    title: "Пакетная обработка",
    text: "Добавляйте сразу несколько фото и отмечайте в списке только нужные изображения.",
  },
  {
    icon: "preview",
    color: "var(--accent)",
    title: "Предпросмотр до обработки",
    text: "Для одного файла показывается крупный предпросмотр, для нескольких — удобный список.",
  },
  {
    icon: "privacy",
    color: "var(--accent)",
    title: "Обработка на сервере",
    text: "Файлы передаются на сервер для конвертации и оптимизации.",
  },
  {
    icon: "limits",
    color: "var(--accent)",
    title: "Понятные лимиты",
    text: "Гостям доступно 10 обработок и 50 МБ на файл. После входа — до 150 обработок и 500 МБ.",
  },
];

const scenarios = [
  {
    icon: "web",
    title: "Для сайтов и соцсетей",
    text: "Подготовьте лёгкие WEBP или AVIF для страниц, публикаций и быстрой отправки.",
  },
  {
    icon: "archive",
    title: "Для архива и печати",
    text: "Сохраните изображения в PNG или TIFF, когда важны детали и совместимость.",
  },
  {
    icon: "share",
    title: "Для повседневного обмена",
    text: "Выберите JPG или другой удобный формат и обработайте сразу целую подборку фото.",
  },
];

const steps = [
  {
    number: "01",
    title: "Добавьте изображения",
    text: "Перетащите фото в область загрузки или выберите их на устройстве.",
  },
  {
    number: "02",
    title: "Настройте результат",
    text: "Выберите формат, настройте разрешение и сжатие, затем отметьте изображения для обработки.",
  },
  {
    number: "03",
    title: "Запустите обработку",
    text: "Нажмите «Конвертировать», чтобы увидеть окно обработки с индикатором прогресса.",
  },
];

function SectionIcon({ name, size = 22 }) {
  const paths = {
    formats: <><rect x="3" y="3" width="8" height="8" rx="2" /><rect x="13" y="3" width="8" height="8" rx="2" /><rect x="3" y="13" width="8" height="8" rx="2" /><rect x="13" y="13" width="8" height="8" rx="2" /></>,
    quality: <><path d="M4 19V5m0 14h16" /><path d="m6 15 4-4 3 2 6-7" /></>,
    batch: <><rect x="8" y="3" width="13" height="15" rx="2" /><path d="M16 21H5a2 2 0 0 1-2-2V7" /><path d="M12 8h5m-5 4h5" /></>,
    preview: <><rect x="3" y="4" width="18" height="16" rx="3" /><circle cx="8.5" cy="9" r="1.5" /><path d="m21 15-5-5L5 20" /></>,
    privacy: <><rect x="4" y="10" width="16" height="11" rx="2" /><path d="M8 10V7a4 4 0 1 1 8 0v3m-4 4v3" /></>,
    limits: <><path d="M12 3 4 6v5c0 5 3.4 8.5 8 10 4.6-1.5 8-5 8-10V6l-8-3Z" /><path d="m9 12 2 2 4-4" /></>,
    web: <><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18" /></>,
    archive: <><path d="M4 7h16v13H4zM3 4h18v3H3z" /><path d="M9 11h6" /></>,
    share: <><circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><path d="m8.7 10.7 6.6-4.4m-6.6 7.4 6.6 4.4" /></>,
    sparkles: <><path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" /><path d="m19 14 .9 2.1L22 17l-2.1.9L19 20l-.9-2.1L16 17l2.1-.9L19 14Z" /></>,
    arrowDown: <><path d="M12 4v15m-6-6 6 6 6-6" /></>,
    check: <path d="m5 12 4 4L19 6" />,
  };

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name]}
    </svg>
  );
}

function Reveal({ as: Element = "div", className = "", delay = 0, direction = "up", children, style = {}, ...props }) {
  const ref = useRef(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const node = ref.current;
    if (!node) return undefined;
    if (!("IntersectionObserver" in window)) {
      setVisible(true);
      return undefined;
    }

    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setVisible(true);
        observer.disconnect();
      }
    }, { rootMargin: "0px 0px -60px 0px" });

    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  return (
    <Element
      ref={ref}
      className={`reveal reveal-${direction} ${visible ? "is-visible" : ""} ${className}`}
      style={{ "--reveal-delay": `${delay}s`, ...style }}
      {...props}
    >
      {children}
    </Element>
  );
}

function SectionHeading({ eyebrow, title, description, icon }) {
  return (
    <div className="info-heading">
      <Reveal className="info-eyebrow">
        <SectionIcon name={icon} size={14} /> {eyebrow}
      </Reveal>
      <Reveal as="h2" className="info-title" delay={0.05}>{title}</Reveal>
      <Reveal as="p" className="info-description" delay={0.12}>{description}</Reveal>
    </div>
  );
}

const marqueeItems = [...formatTicker, ...formatTicker];

function MarqueeGroup({ hidden }) {
  return (
    <div className="marquee-group" aria-hidden={hidden || undefined}>
      {marqueeItems.map((item, index) => (
        <span className="marquee-item" key={`${item}-${index}`}>
          {item}<i />
        </span>
      ))}
    </div>
  );
}

export function FormatMarquee() {
  return (
    <section className="format-marquee" aria-label="Форматы изображений">
      <div className="marquee-layout">
        <span className="marquee-center">7 ФОРМАТОВ</span>
        <div className="marquee-track">
          <div className="marquee-flow">
            <MarqueeGroup hidden={false} />
            <MarqueeGroup hidden />
            <MarqueeGroup hidden />
          </div>
        </div>
      </div>
    </section>
  );
}

export default function InfoSections() {
  return (
    <>
      <section className="info-section" id="features">
        <div className="info-container">
          <SectionHeading
            eyebrow="Возможности"
            icon="sparkles"
            title="Всё для работы с изображениями"
            description="Конвертер собран так, чтобы подготовка фотографий занимала минимум времени."
          />

          <div className="info-card-grid feature-grid">
            {features.map((feature, index) => (
              <Reveal
                as="article"
                className="info-card feature-card"
                key={feature.title}
                delay={index * 0.06}
                style={{ "--card-accent": feature.color }}
              >
                <span className="info-card-icon"><SectionIcon name={feature.icon} /></span>
                <h3>{feature.title}</h3>
                <p>{feature.text}</p>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section className="info-section info-secondary" id="scenarios">
        <div className="info-container">
          <SectionHeading
            eyebrow="Сценарии"
            icon="sparkles"
            title="Один конвертер — разные задачи"
            description="Подберите подходящий формат и степень сжатия для публикации, хранения или отправки."
          />

          <div className="info-card-grid scenario-grid">
            {scenarios.map((scenario, index) => (
              <Reveal
                as="article"
                className="info-card scenario-card"
                key={scenario.title}
                delay={index * 0.08}
              >
                <span className="info-card-icon"><SectionIcon name={scenario.icon} /></span>
                <h3>{scenario.title}</h3>
                <p>{scenario.text}</p>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section className="info-section" id="instructions">
        <div className="info-container">
          <SectionHeading
            eyebrow="Как это работает"
            icon="sparkles"
            title="Три шага до готового изображения"
            description="От загрузки фотографии до выбора параметров — всё в одном окне."
          />

          <div className="info-card-grid steps-grid">
            {steps.map((step, index) => (
              <Reveal
                as="article"
                className="info-card step-card"
                key={step.number}
                delay={index * 0.1}
                direction="left"
              >
                <span className="step-number">{step.number}</span>
                <h3>{step.title}</h3>
                <p>{step.text}</p>
              </Reveal>
            ))}
          </div>

          <Reveal className="info-cta">
            <p className="info-cta-title">Готовы подготовить изображения?</p>
            <p className="info-cta-copy">Добавьте фотографии и настройте конвертацию в рабочем окне.</p>
            <a className="button button-primary info-cta-button" href="#converter">
              Перейти к конвертеру <SectionIcon name="arrowDown" size={16} />
            </a>
          </Reveal>
        </div>
      </section>
    </>
  );
}
