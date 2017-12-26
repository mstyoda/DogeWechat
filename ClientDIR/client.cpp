#include <stdlib.h>  
#include <stdio.h>  
#include <errno.h>  
#include <string.h>  
#include <unistd.h>  
#include <netdb.h>  
#include <sys/socket.h>  
#include <netinet/in.h>  
#include <sys/types.h>  
#include <arpa/inet.h>  
#include <pthread.h>  
      
#define MAXLINE 100;

#define PACKAGE_SIZE 256
#define N_USER 3

#define rep(i,l,r) for (int i=l;i<=r;i++)
#define drep(i,l,r) for (int i=l;i>=r;i--)

void *threadsend(void *vargp);  
void *threadrecv(void *vargp);  
      
int main()
{     
    int *clientfdp;  
    clientfdp = (int *)malloc(sizeof(int));  
    *clientfdp = socket(AF_INET,SOCK_STREAM,0);  
    struct sockaddr_in serveraddr;  
    struct hostent *hp;  
    bzero((char *)&serveraddr,sizeof(serveraddr));  
    serveraddr.sin_family = AF_INET;  
    serveraddr.sin_port = htons(15636);  
    serveraddr.sin_addr.s_addr = inet_addr("127.0.0.1");  
    if(connect(*clientfdp,(struct sockaddr *)&serveraddr,sizeof(serveraddr)) < 0){  
            printf("connect error\n");  
            exit(1);  
    }  
      
    pthread_t tid1,tid2;  
    printf("connected\n");  
    void * s1;
    void * s2;
    pthread_create(&tid1,NULL,threadsend,clientfdp);
    pthread_create(&tid2,NULL,threadrecv,clientfdp);  
    
    pthread_join(tid1,&s1);
    pthread_join(tid2,&s2);

    return EXIT_SUCCESS;  
}  


void getSubString(char *dst, char *src, int l,int r)
{
    int k = 0;
    rep(i,l,r) dst[k++] = src[i];
    //rep(i,l,r) dst[k++] = '0'+k;
    dst[k++] = '\0';
}

void getRequestForLogin(char *name, char *password, char *request)
{
    //command[1] = 'l'
    //state[1] = 'y' or 'n'
    request[0] = 'l';
    rep(i,0,19)
    {
        request[i + 1] = name[i];
        request[i + 21] = password[i];
    }
    //getSubString(request + 1,name,0,19);
    //getSubString(request + 20,password,0,19);

}

void getRequestForQuit(char *request)
{
    //command[1] = 'q'
    request[0] = 'q';
}

void getRequestForSendMessage(char *myName, char *name, char *msg, char *request)
{
    //command[1] = 's'
    //srcName[20] = myName
    //dstName[20] = name
    //msg[215] = msg

    request[0] = 's';
    rep(i,0,19)
    {
        request[i + 1] = myName[i];
        request[i + 21] = name[i];
    }
    rep(i,0,204)
    {
        request[i + 41] = msg[i];
    }
}

void *threadsend(void * vargp)  
{  
    //pthread_t tid2;  
    int connfd = *((int *)vargp);  
      
    int idata;  
    char request[256];
    char name[25];
    char password[25];
    char cmd;
    char myName[25];
    //256-1-20-20 = 215 
    char msg[215];

    while (1) 
    {  
        //printf("me: \n ");
        //fgets(name,25,stdin);
        //fgets(password,25,stdin);
        printf("command: "); scanf("%c",&cmd);
        if (cmd == 'l') //login
        {
            printf("username: "); scanf("%s",name);
            strcpy(myName,name);
            printf("password: "); scanf("%s",password);
            getRequestForLogin(name,password,request);
            printf("send pack = %s\n",request);
            send(connfd,request,256,0);
        }
        else if (cmd == 'q') //quit
        {
            getRequestForQuit(request);
            send(connfd,request,256,0);
            close(connfd);
            break;
        }
        else if (cmd == 's') //send message
        {
            printf("send message to? [name] : "); scanf("%s",name);
            printf("message: "); scanf("%s",msg);
            getRequestForSendMessage(myName,name,msg,request);
            printf("send pack = %s\n",request);
            send(connfd,request,256,0);
        }
    }  
    return NULL;  
}  
      
      
void *threadrecv(void *vargp)  
{  
    int connfd = *((int *)vargp);  
      
    int idata;  
    char name[25];
    char password[25];
    char cmd;
    char myName[25];
    //256-1-20-20 = 215 
    char msg[215];
    char pack[PACKAGE_SIZE];

    while (1) 
    {  
        recv(connfd,pack,PACKAGE_SIZE,0);
        cmd = pack[0];
        if (cmd == 's') //send message
        {
            getSubString(name,pack,1,20);
            getSubString(myName,pack,21,40);
            getSubString(msg,pack,41,255);

            printf("message from? [name] : %s\n",name);
            printf("message: %s\n",msg);
        }
        else
            printf("Server: %s\n",pack);
    }  
    return NULL;  
}  