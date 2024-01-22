const toggleSidebar = ()=>{
	if($(".sidebar").is(":visible")){
		$(".sidebar").css("display","none");
		$(".content").css("margin-left","0%");
	}
	else{
		$(".sidebar").css("display","block");
		$(".content").css("margin-left","20%");
	}
};

const search = () => {
    let query = $("#search-input").val();
    if (query === "") {
        $(".search-result").hide();
    } else {
        let url = `http://localhost:8080/search/${query}`;

        fetch(url)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log(data);
                if (!data || !Array.isArray(data)) {
                    throw new Error('Invalid data format');
                }

                let text = `<div class='list-group'>`;
                data.forEach(contact => {
                    text += `<a href='/user/${contact.cId}/contact/' class='list-group-item list-group-item-action'>${contact.name}</a>`;
                });
                text += `</div>`;
                $(".search-result").html(text);
                $(".search-result").show();
            })
            .catch(error => {
                console.error('Error fetching or processing data:', error);
            });
    }
};

const paymentStart = ()=>{
    let amount = $("#payment_field").val();

    $.ajax(
    {
        url:'/user/create_order',
        data:JSON.stringify({amount:amount,info:'order_request'}),
        contentType:'application/json',
        type:'POST',
        dataType:'json',
        success:function(response){
            console.log(response);
            if(response.status=="created"){
                let options={
                    key:'rzp_test_Ux8gnwfhzTQJmK',
                    amount:response.amount,
                    currency:'INR',
                    name:'Smart Contact Manager',
                    description:'Donation',
                    image:'https://www.learncodewithdurgesh.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Flcwd_logo.45da3818.png&w=1080&q=75',
                    order_id:response.id,
                    handler: function(response){
                        console.log(response.razorpay_payment_id);
                        updatePaymentOnServer(response.razorpay_payment_id,response.razorpay_order_id,'paid')
                    },
                    "prefill": {
                    "name": "",
                    "email": "",
                    "contact": ""
                    },
                    "notes": {
                    "address": "Razorpay Corporate Office"
                    },
                    "theme": {
                    "color": "#3399cc"
                    }
                };
                let rzp1 = new Razorpay(options);
                rzp1.on('payment.failed', function (response){
                Swal.fire({
                  icon: "error",
                  title: "Oops...",
                  text: "Payment Failed.!",
                  footer: ''
                });
                console.log(response.error.code);
                console.log(response.error.description);
                console.log(response.error.source);
                console.log(response.error.step);
                console.log(response.error.reason);
                console.log(response.error.metadata.order_id);
                console.log(response.error.metadata.payment_id);
                });

                rzp1.open();
                    console.log("created");
            }
        },
        error:function(error){
            console.log(error);
            alert("Sorry, but something went wrong...");
        }
    }
    )
};

function updatePaymentOnServer(payment_id,order_id,status){
    $.ajax({
        url:'/user/update_order',
        data:JSON.stringify({payment_id:payment_id,order_id:order_id,status:status}),
        contentType:'application/json',
        type:'POST',
        dataType:'json',
        success: function(response){
        Swal.fire({
          title: "Payment Successful",
          text: "Thankyou for your donation!",
          icon: "success"
        });
        },
        error: function(error){

        }
    })
}

