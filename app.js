const input = document.querySelector('#fileInput');
const canvas = document.querySelector('#canvas');
const ctx = canvas.getContext('2d');
const stage = document.querySelector('#stage');
const empty = document.querySelector('#emptyState');
const start = document.querySelector('#start');
const download = document.querySelector('#download');
const progress = document.querySelector('#progress');
const bar = progress.querySelector('span');

let image = null;
let seed = 0;

const mode = document.querySelector('#mode');
const recognition = document.querySelector('#recognition');
const intensity = document.querySelector('#intensity');

const labels = ['Sehr frei', 'Frei', 'Mittel', 'Erkennbar', 'Sehr klar'];

recognition.oninput = () => {
  document.querySelector('#recognitionValue').value =
    labels[Math.min(4, Math.floor((recognition.value - 20) / 15))];
};

intensity.oninput = () => {
  document.querySelector('#intensityValue').value =
    intensity.value + '%';
};

input.onchange = () => {
  const file = input.files[0];
  if (!file) return;

  const reader = new FileReader();

  reader.onload = () => {
    image = new Image();

    image.onload = () => {
      empty.hidden = true;
      canvas.hidden = false;
      start.disabled = false;
      draw(0);
    };

    image.src = reader.result;
  };

  reader.readAsDataURL(file);
};

stage.ondragover = event => event.preventDefault();

stage.ondrop = event => {
  event.preventDefault();
  input.files = event.dataTransfer.files;
  input.dispatchEvent(new Event('change'));
};

document.querySelector('#randomize').onclick = () => {
  seed = Math.random() * 10000;
  if (image) animate();
};

start.onclick = () => animate();

download.onclick = () => {
  const link = document.createElement('a');
  link.download = 'bildreise-unikat.png';
  link.href = canvas.toDataURL('image/png');
  link.click();
};

function fitCanvas() {
  const max = 1000;
  const scale = Math.min(
    max / image.width,
    max / image.height,
    1
  );

  canvas.width = image.width * scale;
  canvas.height = image.height * scale;
}

function draw(progressValue) {
  if (!image) return;

  fitCanvas();

  const width = canvas.width;
  const height = canvas.height;
  const amount = Number(intensity.value) / 100;
  const keep = Number(recognition.value) / 100;
  const selectedMode = mode.value;

  ctx.save();
  ctx.filter = getFilter(selectedMode, progressValue, amount);
  ctx.globalAlpha = 0.92;
  ctx.drawImage(image, 0, 0, width, height);
  ctx.restore();

  if (selectedMode === 'glitch' && progressValue > 0.2) {
    ctx.globalAlpha = 0.18;

    for (let i = 0; i < 8; i++) {
      const y = Math.random() * height;

      ctx.drawImage(
        canvas,
        0,
        y,
        width,
        Math.random() * 12 + 2,
        (Math.random() - 0.5) * amount * 30,
        y,
        width,
        Math.random() * 12 + 2
      );
    }
  }

  ctx.globalAlpha = 1;

  if (keep < 0.45) {
    ctx.globalAlpha = 0.08;

    ctx.drawImage(
      image,
      Math.sin(progressValue * 8) * amount * 18,
      Math.cos(progressValue * 7) * amount * 12,
      width,
      height
    );

    ctx.globalAlpha = 1;
  }
}

function getFilter(selectedMode, progressValue, amount) {
  const wobble =
    Math.sin(progressValue * 5 + seed) * amount * 10;

  if (selectedMode === 'soft') {
    return `
      saturate(${1 + amount * 0.25})
      contrast(${1 + amount * 0.12})
      brightness(${1 + amount * 0.05})
    `;
  }

  if (selectedMode === 'art') {
    return `
      saturate(${1 + amount})
      contrast(${1 + amount * 0.35})
      sepia(${amount * 0.18})
      hue-rotate(${wobble}deg)
    `;
  }

  if (selectedMode === 'surreal') {
    return `
      saturate(${1 + amount * 1.8})
      contrast(${1 + amount * 0.5})
      hue-rotate(${wobble * 3}deg)
    `;
  }

  if (selectedMode === 'glitch') {
    return `
      saturate(${1 + amount * 1.5})
      contrast(${1 + amount * 0.8})
      hue-rotate(${wobble * 8}deg)
    `;
  }

  return `
    saturate(${1 + amount * 0.8})
    contrast(${1 + amount * 0.25})
    hue-rotate(${wobble}deg)
  `;
}

function animate() {
  if (!image) return;

  start.disabled = true;
  download.disabled = true;
  progress.hidden = false;

  const startedAt = performance.now();
  const duration = 2200;

  function frame(now) {
    const progressValue = Math.min(
      1,
      (now - startedAt) / duration
    );

    draw(progressValue);
    bar.style.width = progressValue * 100 + '%';

    if (progressValue < 1) {
      requestAnimationFrame(frame);
    } else {
      start.disabled = false;
      download.disabled = false;

      setTimeout(() => {
        progress.hidden = true;
      }, 250);
    }
  }

  requestAnimationFrame(frame);
}
