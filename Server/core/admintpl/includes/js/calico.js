var Calico = {};


Calico.ChatClient = Class.create({},{

	initialize: function(){
		this.options = Object.extend({
			user: 'webadmin',
			history: null,
			inputbox: null,
			button:null
		}, arguments[0] || { }); 
		
		this.options.history = $(this.options.history);
		this.options.inputbox = $(this.options.inputbox);
		this.options.button = $(this.options.button);
		
		this.options.button.observe('click',this.getAndSendMsg.bindAsEventListener(this));
		this.options.inputbox.observe('keypress', this.listenForEnterKey.bindAsEventListener(this));
	},
	
	sendChatMessage: function(msg){
	
		this.options.history.value = this.options.history.value + "USER  : "+msg+"\n"; 
	
		new Ajax.Request("/chat?service=web&user="+this.options.user+"&msg="+encodeURIComponent(msg),{
			method:'get',
			onSuccess:this.chatResponseCallback.bind(this),
			onFailure:this.chatFail.bind(this)
		});
	},
	
	getAndSendMsg: function(){
		this.sendChatMessage( this.options.inputbox.value );
		this.options.inputbox.value = "";
	},
	listenForEnterKey: function(event){
		if(event.keyCode==13)
		{
			event.stop();
			this.getAndSendMsg();
		}
	},
	chatResponseCallback: function(resp){
		this.options.history.value = this.options.history.value + "SERVER: "+resp.responseText+"\n"; 
	},
	chatFail: function(resp){
		alert(resp.responseText);
	}

});
