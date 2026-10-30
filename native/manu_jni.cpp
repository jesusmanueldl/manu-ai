#include <jni.h>
#include "llama.h"
#include <chrono>
#include <mutex>
#include <string>
#include <vector>

namespace {
std::mutex g_model_mutex;
std::mutex g_inference_mutex;
llama_model *g_model=nullptr;
std::string g_model_path;
bool g_backend_ready=false;

llama_model *get_model(const char *path){
    std::lock_guard<std::mutex> lock(g_model_mutex);
    if(!g_backend_ready){llama_backend_init();g_backend_ready=true;}
    if(g_model&&g_model_path==path)return g_model;
    if(g_model){llama_model_free(g_model);g_model=nullptr;g_model_path.clear();}
    auto params=llama_model_default_params();
    params.n_gpu_layers=0;
    g_model=llama_model_load_from_file(path,params);
    if(g_model)g_model_path=path;
    return g_model;
}
}

extern "C" JNIEXPORT jstring JNICALL Java_com_manu_ai_LocalModel_nativeGenerateStream(JNIEnv *env,jclass,jstring jm,jstring js,jstring jp,jobject callback){
    std::lock_guard<std::mutex> inference_lock(g_inference_mutex);
    const char *mpath=env->GetStringUTFChars(jm,nullptr);const char *system=env->GetStringUTFChars(js,nullptr);const char *user=env->GetStringUTFChars(jp,nullptr);
    std::string prompt="<|im_start|>system\n"+std::string(system)+"<|im_end|>\n<|im_start|>user\n"+std::string(user)+"<|im_end|>\n<|im_start|>assistant\n";
    std::string out;auto started=std::chrono::steady_clock::now();auto *model=get_model(mpath);
    jclass cbClass=env->GetObjectClass(callback);jmethodID tokenMethod=env->GetMethodID(cbClass,"token","(Ljava/lang/String;)V");jmethodID firstMethod=env->GetMethodID(cbClass,"firstToken","(J)V");jmethodID metricsMethod=env->GetMethodID(cbClass,"metrics","(JIJJ)V");
    long long firstMs=0;int generated=0;int promptTokens=0;
    if(!model)out="ERROR: no se pudo cargar el modelo";else{
        auto *v=llama_model_get_vocab(model);int np=-llama_tokenize(v,prompt.c_str(),(int)prompt.size(),nullptr,0,true,true);promptTokens=np;
        if(np<=0||np>=1000)out="ERROR: el contexto supera el límite de 1024 tokens";else{std::vector<llama_token> tokens(np);llama_tokenize(v,prompt.c_str(),(int)prompt.size(),tokens.data(),np,true,true);auto cp=llama_context_default_params();cp.n_ctx=1024;cp.n_batch=np;cp.no_perf=true;auto *ctx=llama_init_from_model(model,cp);if(!ctx)out="ERROR: no se pudo crear el contexto";else{
            auto *sampler=llama_sampler_chain_init(llama_sampler_chain_default_params());llama_sampler_chain_add(sampler,llama_sampler_init_temp(.35f));llama_sampler_chain_add(sampler,llama_sampler_init_dist(42));auto *batch=llama_batch_ext_init(ctx);
            for(int i=0;i<np;i++){int x=llama_batch_ext_add_token(batch,0,tokens[i]);llama_pos pos=i;llama_batch_ext_set_pos(batch,x,&pos);}llama_batch_ext_set_output_logits(batch,np-1,true);
            if(llama_process(ctx,LLAMA_PROCESS_TYPE_DECODE,batch)!=0)out="ERROR: falló el procesamiento del prompt";else for(int i=0;i<96&&out.size()<1800;i++){
                auto id=llama_sampler_sample(sampler,ctx,-1);if(llama_vocab_is_eog(v,id))break;char piece[256];int n=llama_token_to_piece(v,id,piece,sizeof(piece),0,true);if(n>0){if(generated==0){firstMs=std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now()-started).count();if(firstMethod)env->CallVoidMethod(callback,firstMethod,(jlong)firstMs);}out.append(piece,n);generated++;if(tokenMethod){jstring value=env->NewStringUTF(std::string(piece,n).c_str());env->CallVoidMethod(callback,tokenMethod,value);env->DeleteLocalRef(value);}}
                llama_batch_ext_clear(batch);int x=llama_batch_ext_add_token(batch,0,id);llama_pos pos=np+i;llama_batch_ext_set_pos(batch,x,&pos);llama_batch_ext_set_output_logits(batch,x,true);if(llama_process(ctx,LLAMA_PROCESS_TYPE_DECODE,batch)!=0)break;}
            llama_batch_ext_free(batch);llama_sampler_free(sampler);llama_free(ctx);
        }}
    }
    long long totalMs=std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now()-started).count();if(metricsMethod)env->CallVoidMethod(callback,metricsMethod,(jlong)promptTokens,(jint)generated,(jlong)firstMs,(jlong)totalMs);
    env->ReleaseStringUTFChars(jm,mpath);env->ReleaseStringUTFChars(js,system);env->ReleaseStringUTFChars(jp,user);return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL Java_com_manu_ai_LocalModel_nativeShutdown(JNIEnv *,jclass){std::lock_guard<std::mutex> lock(g_model_mutex);if(g_model){llama_model_free(g_model);g_model=nullptr;g_model_path.clear();}if(g_backend_ready){llama_backend_free();g_backend_ready=false;}}
