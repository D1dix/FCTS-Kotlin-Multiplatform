package com.jetbrains.greeting.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputWidget
import com.jetbrains.greeting.viewmodels.CartViewModel
import com.jetbrains.greeting.viewmodels.PaymentStatus
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import android.util.Log


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StripePaymentForm(
    amount: Double,
    cartId: String,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit,
    cartViewModel: CartViewModel
) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }
    val paymentStatus by cartViewModel.paymentStatus.collectAsState()
    var isCardValid by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Estados Unidos") }
    var postalCode by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("EUR") }
    var cardWidget: CardInputWidget? by remember { mutableStateOf(null) }

    val stripe = remember {
        Stripe(context, PaymentConfiguration.getInstance(context).publishableKey)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Total a pagar: $${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            "Información de la tarjeta",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/Old_Visa_Logo.svg/2560px-Old_Visa_Logo.svg.png",
                contentDescription = "Visa",
                modifier = Modifier.height(40.dp)
            )
            AsyncImage(
                model = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2a/Mastercard-logo.svg/960px-Mastercard-logo.svg.png",
                contentDescription = "MasterCard",
                modifier = Modifier.height(40.dp)
            )
            AsyncImage(
                model = "https://cdn-assets-eu.frontify.com/s3/frontify-enterprise-files-eu/eyJwYXRoIjoiYWR5ZW5cL2ZpbGVcLzRlVmlmam51YzM1WGRWUmNvc0NoLnN2ZyJ9:adyen:MEsDzXieFhW6x967c-P4fspRtv3hIaEzCubqlYo_qs0",
                contentDescription = "Discover",
                modifier = Modifier.height(40.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    CardInputWidget(ctx).also { widget ->
                        cardWidget = widget
                        widget.setCardValidCallback { isValid, _ ->
                            isCardValid = isValid
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("País o región", style = MaterialTheme.typography.titleMedium)
            DropdownMenuBox(
                selected = country,
                options = listOf("Estados Unidos", "España", "México", "Argentina"),
                onOptionSelected = { country = it }
            )

            Text("Código postal", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = postalCode,
                onValueChange = { postalCode = it },
                label = { Text("Código postal") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Moneda", style = MaterialTheme.typography.titleMedium)
            DropdownMenuBox(
                selected = currency,
                options = listOf("EUR", "USD"),
                onOptionSelected = { currency = it }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (paymentStatus) {
            PaymentStatus.LOADING -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            PaymentStatus.SUCCESS -> {
                Text(
                    "¡Pago realizado con éxito!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                onPaymentSuccess()
            }
            PaymentStatus.ERROR -> {
                Text(
                    "Error al procesar el pago",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            null -> {
                Button(
                    onClick = {
                        if (!isCardValid) {
                            return@Button
                        }
                        isProcessing = true
                        val params = cardWidget?.paymentMethodCreateParams
                        if (params != null) {
                            Log.d("StripePaymentForm", "🔄 Iniciando creación de método de pago")
                            stripe.createPaymentMethod(
                                params,
                                callback = object : ApiResultCallback<PaymentMethod> {
                                    override fun onSuccess(result: PaymentMethod) {
                                        val paymentMethodId = result.id
                                        Log.d("StripePaymentForm", "✅ Método de pago creado exitosamente. ID: $paymentMethodId")
                                        if (paymentMethodId != null) {
                                            Log.d("StripePaymentForm", "🔄 Iniciando procesamiento de pago con ID: $paymentMethodId")
                                            cartViewModel.processStripePayment(cartId, amount, paymentMethodId, email)
                                        } else {
                                            Log.e("StripePaymentForm", "❌ ID de método de pago nulo")
                                            isProcessing = false
                                        }
                                    }
                                    override fun onError(e: Exception) {
                                        Log.e("StripePaymentForm", "❌ Error al crear método de pago: ${e.message}")
                                        isProcessing = false
                                    }
                                }
                            )
                        } else {
                            Log.e("StripePaymentForm", "❌ Parámetros de pago nulos")
                            isProcessing = false
                        }
                    },
                    enabled = !isProcessing && isCardValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        "Pagar",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        cartViewModel.cancelPayment(cartId)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        "Cancelar",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownMenuBox(selected: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                selected,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
