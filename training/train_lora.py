"""Ajuste LoRA inicial para MANU. Requiere las dependencias de requirements-pc.txt."""
import json
from pathlib import Path
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, TrainingArguments, Trainer, DataCollatorForLanguageModeling
from peft import LoraConfig, get_peft_model

ROOT = Path(__file__).parent
cfg = json.loads((ROOT / "config.json").read_text(encoding="utf-8"))
rows = [json.loads(x) for x in (ROOT / "data" / "manu_train.jsonl").read_text(encoding="utf-8").splitlines() if x.strip()]
tokenizer = AutoTokenizer.from_pretrained(cfg["base_model"])
if tokenizer.pad_token is None: tokenizer.pad_token = tokenizer.eos_token
texts = [tokenizer.apply_chat_template(r["messages"], tokenize=False, add_generation_prompt=False) for r in rows]
encoded = tokenizer(texts, truncation=True, max_length=cfg["max_seq_length"], padding=True)
class Rows(torch.utils.data.Dataset):
    def __len__(self): return len(encoded["input_ids"])
    def __getitem__(self, i):
        item = {k: torch.tensor(v[i]) for k, v in encoded.items()}
        item["labels"] = item["input_ids"].clone()
        return item
model = AutoModelForCausalLM.from_pretrained(cfg["base_model"], torch_dtype=torch.float16, device_map="auto")
model = get_peft_model(model, LoraConfig(r=cfg["lora_r"], lora_alpha=cfg["lora_alpha"], lora_dropout=cfg["lora_dropout"], target_modules=cfg["target_modules"], task_type="CAUSAL_LM"))
model.print_trainable_parameters()
args = TrainingArguments(output_dir=cfg["output_dir"], num_train_epochs=cfg["epochs"], learning_rate=cfg["learning_rate"], per_device_train_batch_size=cfg["batch_size"], gradient_accumulation_steps=cfg["gradient_accumulation"], fp16=True, logging_steps=1, save_strategy="epoch", report_to="none")
Trainer(model=model, args=args, train_dataset=Rows(), data_collator=DataCollatorForLanguageModeling(tokenizer, mlm=False)).train()
model.save_pretrained(cfg["output_dir"]); tokenizer.save_pretrained(cfg["output_dir"])
print(f"adapter guardado en {cfg['output_dir']}")
