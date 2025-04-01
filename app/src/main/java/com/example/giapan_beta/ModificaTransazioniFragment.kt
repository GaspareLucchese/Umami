package com.example.giapan_beta

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.giapan_beta.databinding.FragmentModificaTransazioniBinding

class ModificaTransazioniFragment : Fragment() {

    private lateinit var binding: FragmentModificaTransazioniBinding
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inizializza il binding
        binding = FragmentModificaTransazioniBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        binding.modificaCartaText.setText(Html.fromHtml("<u>MODIFICA LA TUA CARTA</u>"))


        // Popola i campi con i dati esistenti
        viewModel.datiRegistrazione.observe(viewLifecycleOwner) { userData ->
            binding.intestatarioInserito.setText(userData.intestatario)
            binding.numeroCartaInserito.setText(userData.carta.toString())
            binding.scadenzaCartaInserita.setText(String.format("%02d/%02d", userData.meseScadenza, userData.annoScadenza))
            binding.codiceCartaInserito.setText(userData.codiceCvv.toString())
        }

        binding.scadenzaCartaInserita.apply {
            // Imposta il filtro di input per limitare la lunghezza a 5 caratteri
            filters = arrayOf(InputFilter.LengthFilter(5))

            // Aggiungi il TextWatcher per formattare la data di scadenza
            addTextChangedListener(object : TextWatcher {
                private var isFormatting = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isFormatting) return

                    val input = s.toString().replace("/", "")
                    if (input.length <= 4) {
                        isFormatting = true
                        val formattedInput = if (input.length > 2) {
                            "${input.substring(0, 2)}/${input.substring(2)}"
                        } else {
                            input
                        }
                        setText(formattedInput)
                        setSelection(formattedInput.length)
                        isFormatting = false
                    }
                }
            })
        }

        // Imposta il click listener per il pulsante "conferma"
        binding.confermaBottone.setOnClickListener {
            val intestatario = binding.intestatarioInserito.text.toString()
            val numeroCarta = binding.numeroCartaInserito.text.toString()
            val dataScadenza = binding.scadenzaCartaInserita.text.toString()
            val cvv = binding.codiceCartaInserito.text.toString()

            if (intestatario.isEmpty() || numeroCarta.isEmpty() || dataScadenza.isEmpty() || cvv.isEmpty())
            {
                Toast.makeText(context, "Compilare tutti i campi", Toast.LENGTH_SHORT).show()
            }
            else if (!intestatario.contains(Regex("^[A-Za-z]+ [A-Za-z]+\$"))) {
                Toast.makeText(context, "Inserire un intestatario valido", Toast.LENGTH_SHORT).show()
                binding.intestatarioInserito.requestFocus()
            }
            else if (!numeroCarta.contains(Regex("^[0-9]+\$")) || (numeroCarta.length < 16))
            {
                Toast.makeText(context, "Inserire tutte le cifre della carta", Toast.LENGTH_SHORT).show()
                binding.numeroCartaInserito.requestFocus()
            }
            else if ((cvv.length < 3) || (!cvv.contains(Regex("^[0-9]+\$"))))
            {
                Toast.makeText(context, "Inserire tutte le cifre della codice CVV", Toast.LENGTH_SHORT).show()
                binding.codiceCartaInserito.requestFocus()
            }
            else
            {
                try {
                    val cartaULong = numeroCarta.toULong()
                    val meseAnnoScadenza = dataScadenza.split("/")
                    if (meseAnnoScadenza.size != 2) {
                        Toast.makeText(context, "Inserire una data di scadenza valida", Toast.LENGTH_SHORT).show()
                        binding.scadenzaCartaInserita.requestFocus()
                    } else {
                        val meseScadenza = meseAnnoScadenza[0].toInt()
                        val annoScadenza = meseAnnoScadenza[1].toInt()
                        if (meseScadenza > 12) {
                            Toast.makeText(context, "Inserire una data di scadenza accettabile", Toast.LENGTH_SHORT).show()
                            binding.scadenzaCartaInserita.requestFocus()
                        } else {
                            val email = sharedPref.getString("email", null)
                            if (email != null) {
                                // Aggiorna i dati nel ViewModel
                                viewModel.updateTransazioni(
                                    email,
                                    intestatario,
                                    numeroCarta,
                                    dataScadenza,
                                    cvv
                                )
                                val fragmentManager = parentFragmentManager
                                val fragmentTransaction = fragmentManager.beginTransaction()

                                fragmentManager.popBackStack()
                                fragmentTransaction.commit()

                            } else {
                                Toast.makeText(context, "Email non trovata", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "La data di scadenza della carta non è stata inserita correttamente", Toast.LENGTH_SHORT).show()
                    binding.scadenzaCartaInserita.requestFocus()
                }
            }
        }
    }
}