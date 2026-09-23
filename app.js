const input=document.querySelector('#fileInput'),canvas=document.querySelector('#canvas'),ctx=canvas.getContext('2d'),stage=document.querySelector('#stage'),empty=document.querySelector('#emptyState'),start=document.querySelector('#start'),download=document.querySelector('#download'),progress=document.querySelector('#progress'),bar=progress.querySelector('span');

const mode=document.querySelector('#mode'),recognition=document.querySelector('#recognition'),intensity=document.querySelector('#intensity');

let image=null;
let seed=Math.random()*99999;
let base=document.createElement('canvas');
let baseCtx=base.getContext('2d');

const labels=['Sehr frei','Frei','Mittel','Erkennbar','Sehr klar'];

recognition.oninput=()=>{
  document.querySelector('#recognitionValue').value=
    labels[Math.min(4,Math.floor((recognition.value-20)/15))];
};

intensity.oninput=()=>{
  document.querySelector('#intensityValue').value=intensity.value+'%';
};

input.onchange=()=>{
  const file=input.files[0];
  if(!file)return;

  const reader=new FileReader();

  reader.onload=()=>{
    image=new Image();

    image.onload=()=>{
      const scale=Math.min(1000/image.width,1000/image.height,1);

      canvas.width=base.width=image.width*scale;
      canvas.height=base.height=image.height*scale;

      baseCtx.drawImage(image,0,0,base.width,base.height);

      empty.hidden=true;
      canvas.hidden=false;
      start.disabled=false;

      render(1);
    };

    image.src=reader.result;
  };

  reader.readAsDataURL(file);
};

stage.ondragover=e=>e.preventDefault();

stage.ondrop=e=>{
  e.preventDefault();
  input.files=e.dataTransfer.files;
  input.dispatchEvent(new Event('change'));
};

document.querySelector('#randomize').onclick=()=>{
  seed=Math.random()*99999;
  if(image)animate();
};

start.onclick=()=>animate();

download.onclick=()=>{
  const link=document.createElement('a');
  link.download='bildreise-unikat.png';
  link.href=canvas.toDataURL('image/png');
  link.click();
};

function rand(n){
  const x=Math.sin(seed+n*12.9898)*43758.5453;
  return x-Math.floor(x);
}

function render(p){
  if(!image)return;

  const w=canvas.width;
  const h=canvas.height;
  const a=Number(intensity.value)/100;
  const keep=Number(recognition.value)/100;
  const m=mode.value;

  ctx.clearRect(0,0,w,h);

  ctx.save();
  ctx.filter=filters(m,p,a);
  ctx.globalAlpha=.96;
  ctx.drawImage(base,0,0);
  ctx.restore();

  if(m!=='soft')deform(p,a,keep,m);
  if(m==='glitch'||(m==='fantasy'&&p>.78))glitch(p,a);
  if(m==='geometry'||m==='surreal'||(m==='fantasy'&&p>.55))geometry(p,a);
  if(m==='nature')nature(p,a);
  if(m==='retro')retro(p,a);
  if(m==='art'||m==='fantasy')texture(p,a);
  if(m==='fantasy')fantasyGlow(p,a);

  if((m==='fantasy'||m==='surreal'||m==='art')&&p>.3){
    skinAbstract(p,a,keep);
  }
}

function filters(m,p,a){
  const hue=(rand(1)*360-180)*a*p;

  if(m==='soft'){
    return `saturate(${1+a*.25}) contrast(${1+a*.1})`;
  }

  if(m==='art'){
    return `saturate(${1.2+a}) contrast(${1+a*.4}) sepia(${a*.2}) hue-rotate(${hue/2}deg)`;
  }

  if(m==='retro'){
    return `saturate(${1-a*.25}) contrast(${1+a*.45}) sepia(${.25+a*.35})`;
  }

  if(m==='nature'){
    return `saturate(${1.2+a*1.8}) contrast(${1+a*.25}) hue-rotate(${hue/3}deg)`;
  }

  if(m==='glitch'){
    return `saturate(${1+a*1.8}) contrast(${1+a}) hue-rotate(${hue*2}deg)`;
  }

  return `saturate(${1+a*1.5}) contrast(${1+a*.5}) hue-rotate(${hue}deg)`;
}

function deform(p,a,keep,m){
  const w=canvas.width;
  const h=canvas.height;
  const slices=22;

  ctx.save();
  ctx.globalAlpha=.35+a*.45;

  for(let i=0;i<slices;i++){
    const y=i*h/slices;
    const sh=h/slices+1;
    const wave=Math.sin(i*.7+p*9+seed)*
      a*p*(m==='surreal'?55:28)*(1-keep*.45);

    ctx.drawImage(base,0,y,w,sh,wave,y,w,sh);
  }

  ctx.restore();

  if(m==='fantasy'||m==='surreal'){
    ctx.save();
    ctx.globalAlpha=.2*a*p;

    for(let i=0;i<6;i++){
      const x=rand(i+20)*w;
      const y=rand(i+40)*h;
      const r=(.08+rand(i+60)*.2)*Math.min(w,h);

      ctx.beginPath();
      ctx.arc(x,y,r,0,Math.PI*2);
      ctx.clip();

      ctx.translate(
        (rand(i+80)-.5)*a*80,
        (rand(i+90)-.5)*a*80
      );

      ctx.rotate((rand(i+100)-.5)*a*.5);
      ctx.drawImage(base,0,0);

      ctx.restore();
      ctx.save();
      ctx.globalAlpha=.2*a*p;
    }

    ctx.restore();
  }
}

function glitch(p,a){
  ctx.save();

  for(let i=0;i<Math.floor(5+a*18);i++){
    const y=rand(i+200)*canvas.height;
    const sh=2+rand(i+250)*18;

    ctx.globalAlpha=.15+a*.18;

    ctx.drawImage(
      canvas,
      0,y,canvas.width,sh,
      (rand(i+300)-.5)*a*90,
      y,
      canvas.width,
      sh
    );
  }

  ctx.restore();
}

function geometry(p,a){
  ctx.save();
  ctx.globalAlpha=.12+a*.16;

  const w=canvas.width;
  const h=canvas.height;

  for(let i=0;i<3;i++){
    ctx.translate(w/2,h/2);
    ctx.rotate((i+1)*Math.PI/2);
    ctx.scale(1-a*.15,1-a*.15);
    ctx.drawImage(base,-w/2,-h/2);
    ctx.setTransform(1,0,0,1,0,0);
  }

  ctx.restore();
}

function texture(p,a){
  ctx.save();
  ctx.globalAlpha=.08+a*.12;

  for(let i=0;i<90;i++){
    ctx.fillStyle=`hsl(${rand(i+500)*360} 80% 70%)`;
    ctx.beginPath();
    ctx.arc(
      rand(i+600)*canvas.width,
      rand(i+700)*canvas.height,
      1+rand(i+800)*a*10,
      0,
      Math.PI*2
    );
    ctx.fill();
  }

  ctx.restore();
}

function nature(p,a){
  ctx.save();
  ctx.globalAlpha=.16+a*.18;
  ctx.strokeStyle='rgba(120,210,170,.7)';
  ctx.lineWidth=1+a*3;

  for(let i=0;i<16;i++){
    const x=rand(i+900)*canvas.width;
    const y=rand(i+950)*canvas.height;

    ctx.beginPath();
    ctx.moveTo(x,y);
    ctx.bezierCurveTo(
      x+30*a,y-50*a,
      x-40*a,y-90*a,
      x+Math.sin(p*8+i)*70*a,
      y-130*a
    );
    ctx.stroke();
  }

  ctx.restore();
}

function retro(p,a){
  ctx.save();
  ctx.globalAlpha=.12+a*.12;

  const h=canvas.height;

  for(let y=0;y<h;y+=4+Math.floor(a*3)){
    ctx.fillStyle=y%8===0
      ?'rgba(20,10,5,.45)'
      :'rgba(255,220,160,.08)';

    ctx.fillRect(0,y,canvas.width,1);
  }

  ctx.restore();
}

function fantasyGlow(p,a){
  if(p<.22||p>.92)return;

  ctx.save();

  const g=ctx.createRadialGradient(
    canvas.width*.5,
    canvas.height*.45,
    0,
    canvas.width*.5,
    canvas.height*.45,
    Math.max(canvas.width,canvas.height)*.7
  );

  g.addColorStop(
    0,
    `hsla(${rand(1200)*360},90%,70%,${.08+a*.1})`
  );

  g.addColorStop(1,'rgba(0,0,0,0)');

  ctx.fillStyle=g;
  ctx.globalCompositeOperation='screen';
  ctx.fillRect(0,0,canvas.width,canvas.height);

  ctx.restore();
}

function skinAbstract(p,a,keep){
  const w=canvas.width;
  const h=canvas.height;
  const step=Math.max(8,Math.floor(18-a*8));
  const pixels=baseCtx.getImageData(0,0,w,h).data;

  ctx.save();
  ctx.globalCompositeOperation='screen';
  ctx.globalAlpha=(.12+a*.32)*(1-keep*.45);

  for(let y=0;y<h;y+=step){
    for(let x=0;x<w;x+=step){
      const i=(y*w+x)*4;
      const r=pixels[i];
      const g=pixels[i+1];
      const b=pixels[i+2];

      if(isSkinLike(r,g,b)){
        const size=step*(1.3+rand(x+y)*2.5)*p;

        ctx.fillStyle=
          `hsla(${(r+g+b+seed)%360},85%,${52+rand(x*y+1)*25}%,.8)`;

        ctx.beginPath();
        ctx.moveTo(x,y);
        ctx.lineTo(
          x+size,
          y+Math.sin(seed+x)*size*.7
        );
        ctx.lineTo(x+size*.55,y+size);
        ctx.lineTo(x-size*.2,y+size*.55);
        ctx.closePath();
        ctx.fill();
      }
    }
  }

  ctx.restore();
}

function isSkinLike(r,g,b){
  const max=Math.max(r,g,b);
  const min=Math.min(r,g,b);

  return r>70 &&
    g>35 &&
    b>20 &&
    r>g*1.08 &&
    g>b*1.12 &&
    max-min>18 &&
    max-min<190;
}

function animate(){
  if(!image)return;

  start.disabled=true;
  download.disabled=true;
  progress.hidden=false;

  const started=performance.now();
  const duration=3600;

  function frame(now){
    const p=Math.min(1,(now-started)/duration);

    render(p);
    bar.style.width=p*100+'%';

    if(p<1){
      requestAnimationFrame(frame);
    }else{
      start.disabled=false;
      download.disabled=false;

      setTimeout(()=>{
        progress.hidden=true;
      },350);
    }
  }

  requestAnimationFrame(frame);
}
